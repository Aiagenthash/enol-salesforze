package com.enol.salesforze.ui.attendance

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.AttendanceEntity
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import com.enol.salesforze.data.location.LocationServiceController
import com.enol.salesforze.data.remote.models.EndShiftRequest
import com.enol.salesforze.data.remote.models.StartShiftRequest
import com.enol.salesforze.data.remote.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

/**
 * Steps in the attendance and shift lifecycle state machine.
 */
enum class ShiftStep {
    START_DAY_INITIAL,
    CAPTURE_SELFIE,
    CAPTURE_ODOMETER,
    CONFIRM_METRICS,
    SUBMITTING,
    ACTIVE_SHIFT,
    END_DAY_ODOMETER,
    SHIFT_SUMMARY
}

/**
 * UI State for Field Shift Attendance workflows.
 */
data class AttendanceUiState(
    val currentStep: ShiftStep = ShiftStep.START_DAY_INITIAL,
    val selfieUri: Uri? = null,
    val odometerPhotoUri: Uri? = null,
    val odometerReading: String = "",
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAccuracyMeters: Float? = null,
    val isGpsLocked: Boolean = false,
    val isGpsAcquiring: Boolean = false,
    val activeShift: AttendanceEntity? = null,
    val endOdometerReading: String = "",
    val endOdometerPhotoUri: Uri? = null,
    val endGpsLatitude: Double? = null,
    val endGpsLongitude: Double? = null,
    val shiftDurationHours: Double? = null,
    val totalKmDriven: Double? = null,
    val isSubmitting: Boolean = false,
    val isMockBlocked: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val locationController: LocationServiceController,
    private val gpsLockValidator: GpsLockValidator,
    @ApplicationContext private val context: Context,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    init {
        observeActiveShift()
        observeMockLocationBlock()
    }

    private fun observeActiveShift() {
        viewModelScope.launch {
            val userId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
            if (userId != null) {
                database.attendanceDao().getActiveAttendanceForUser(userId).collect { activeShift ->
                    _uiState.update { state ->
                        if (activeShift != null) {
                            state.copy(
                                activeShift = activeShift,
                                currentStep = if (state.currentStep == ShiftStep.END_DAY_ODOMETER) {
                                    ShiftStep.END_DAY_ODOMETER
                                } else if (state.currentStep == ShiftStep.SHIFT_SUMMARY) {
                                    ShiftStep.SHIFT_SUMMARY
                                } else {
                                    ShiftStep.ACTIVE_SHIFT
                                }
                            )
                        } else {
                            if (state.currentStep == ShiftStep.ACTIVE_SHIFT) {
                                state.copy(activeShift = null, currentStep = ShiftStep.START_DAY_INITIAL)
                            } else {
                                state.copy(activeShift = null)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeMockLocationBlock() {
        viewModelScope.launch {
            locationController.isMockLocationBlockedFlow.collect { isBlocked ->
                _uiState.update { it.copy(isMockBlocked = isBlocked) }
            }
        }
    }

    /**
     * Acquires high-accuracy GPS satellite lock and validates anti-spoofing constraints.
     */
    fun acquireGpsLock() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGpsAcquiring = true, errorMessage = null) }
            val result = gpsLockValidator.acquireValidGpsLock()
            result.onSuccess { location ->
                _uiState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = true,
                        gpsLatitude = location.latitude,
                        gpsLongitude = location.longitude,
                        gpsAccuracyMeters = location.accuracy,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = false,
                        errorMessage = error.message ?: "Failed to acquire GPS lock"
                    )
                }
            }
        }
    }

    fun onSelfieCaptured(uri: Uri) {
        _uiState.update {
            it.copy(
                selfieUri = uri,
                currentStep = ShiftStep.CAPTURE_ODOMETER,
                errorMessage = null
            )
        }
    }

    fun onOdometerPhotoCaptured(uri: Uri) {
        _uiState.update {
            it.copy(
                odometerPhotoUri = uri,
                currentStep = ShiftStep.CONFIRM_METRICS,
                errorMessage = null
            )
        }
    }

    fun onOdometerReadingChanged(reading: String) {
        _uiState.update { it.copy(odometerReading = reading) }
    }

    fun onEndOdometerPhotoCaptured(uri: Uri) {
        _uiState.update { it.copy(endOdometerPhotoUri = uri, errorMessage = null) }
    }

    fun onEndOdometerReadingChanged(reading: String) {
        _uiState.update { it.copy(endOdometerReading = reading) }
    }

    fun navigateToStep(step: ShiftStep) {
        _uiState.update { it.copy(currentStep = step, errorMessage = null) }
    }

    /**
     * Submits shift start: persists record in local Room DB, enqueues sync mutation,
     * and starts foreground location tracking service.
     */
    fun submitStartShift() {
        val state = _uiState.value

        if (state.selfieUri == null) {
            _uiState.update { it.copy(errorMessage = "Facial selfie verification is required.") }
            return
        }
        if (state.odometerPhotoUri == null) {
            _uiState.update { it.copy(errorMessage = "Vehicle odometer photo is required.") }
            return
        }
        val startKm = state.odometerReading.toDoubleOrNull()
        if (startKm == null || startKm < 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid starting odometer reading in km.") }
            return
        }
        val lat = state.gpsLatitude
        val lng = state.gpsLongitude
        if (!state.isGpsLocked || lat == null || lng == null) {
            _uiState.update { it.copy(errorMessage = "A high-accuracy GPS lock is required before starting the shift.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val userId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
                    ?: throw IllegalStateException("User session not found. Please log in.")

                val attendanceId = UUID.randomUUID().toString()
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                val attendanceEntity = AttendanceEntity(
                    id = attendanceId,
                    userId = userId,
                    date = todayDate,
                    shiftStart = nowIso,
                    shiftEnd = null,
                    startLatitude = lat,
                    startLongitude = lng,
                    startSelfieUrl = state.selfieUri.toString(),
                    startOdometerReading = startKm,
                    startOdometerPhotoUrl = state.odometerPhotoUri.toString(),
                    status = "ACTIVE",
                    updatedAt = nowIso
                )

                val startShiftReq = StartShiftRequest(
                    startLatitude = lat,
                    startLongitude = lng,
                    startSelfieUrl = state.selfieUri.toString(),
                    startOdometerReading = startKm,
                    startOdometerPhotoUrl = state.odometerPhotoUri.toString(),
                    notes = null
                )

                val syncItem = PendingSyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "attendance.start",
                    entityId = attendanceId,
                    action = "START_SHIFT",
                    payloadJson = json.encodeToString(StartShiftRequest.serializer(), startShiftReq),
                    idempotencyKey = "start_shift_${attendanceId}",
                    attemptCount = 0,
                    lastAttemptAt = null,
                    errorMessage = null,
                    createdAt = nowIso
                )

                withContext(Dispatchers.IO) {
                    database.attendanceDao().insertAttendance(attendanceEntity)
                    database.pendingSyncQueueDao().insertSyncItem(syncItem)
                }

                // Launch continuous foreground GPS tracking service
                locationController.startTracking()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        activeShift = attendanceEntity,
                        currentStep = ShiftStep.ACTIVE_SHIFT,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to start shift."
                    )
                }
            }
        }
    }

    /**
     * Submits shift end: records closing odometer photo/reading, calculates shift duration
     * and total distance, stops location tracking, and enqueues sync mutation.
     */
    fun submitEndShift() {
        val state = _uiState.value
        val activeShift = state.activeShift ?: run {
            _uiState.update { it.copy(errorMessage = "No active shift found to close.") }
            return
        }

        if (state.endOdometerPhotoUri == null) {
            _uiState.update { it.copy(errorMessage = "Closing vehicle odometer photo is required.") }
            return
        }

        val endKm = state.endOdometerReading.toDoubleOrNull()
        if (endKm == null || endKm < activeShift.startOdometerReading) {
            _uiState.update {
                it.copy(
                    errorMessage = "Closing odometer reading ($endKm km) must be greater than or equal to start reading (${activeShift.startOdometerReading} km)."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                // Acquire fresh end location
                val endGpsResult = gpsLockValidator.acquireValidGpsLock()
                val endLocation = endGpsResult.getOrNull()
                val endLat = endLocation?.latitude ?: activeShift.startLatitude
                val endLng = endLocation?.longitude ?: activeShift.startLongitude

                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                // Compute shift duration in hours
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val startTime = try {
                    isoFormat.parse(activeShift.shiftStart)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }
                val durationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
                val durationHours = Math.round((durationMs / (1000.0 * 60.0 * 60.0)) * 100.0) / 100.0
                val distanceTraveledKm = Math.round((endKm - activeShift.startOdometerReading) * 10.0) / 10.0

                withContext(Dispatchers.IO) {
                    database.attendanceDao().endShift(
                        attendanceId = activeShift.id,
                        shiftEnd = nowIso,
                        endLatitude = endLat,
                        endLongitude = endLng,
                        endSelfieUrl = activeShift.startSelfieUrl,
                        endOdometer = endKm,
                        endOdometerPhotoUrl = state.endOdometerPhotoUri.toString(),
                        totalHours = durationHours,
                        updatedAt = nowIso
                    )

                    val endShiftReq = EndShiftRequest(
                        endLatitude = endLat,
                        endLongitude = endLng,
                        endSelfieUrl = activeShift.startSelfieUrl,
                        endOdometerReading = endKm,
                        endOdometerPhotoUrl = state.endOdometerPhotoUri.toString(),
                        notes = null
                    )

                    val syncItem = PendingSyncQueueEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "attendance.end",
                        entityId = activeShift.id,
                        action = "END_SHIFT",
                        payloadJson = json.encodeToString(EndShiftRequest.serializer(), endShiftReq),
                        idempotencyKey = "end_shift_${activeShift.id}",
                        attemptCount = 0,
                        lastAttemptAt = null,
                        errorMessage = null,
                        createdAt = nowIso
                    )
                    database.pendingSyncQueueDao().insertSyncItem(syncItem)
                }

                // Stop foreground location service
                locationController.stopTracking()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        currentStep = ShiftStep.SHIFT_SUMMARY,
                        shiftDurationHours = durationHours,
                        totalKmDriven = distanceTraveledKm,
                        activeShift = null,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Failed to close shift."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetToStart() {
        _uiState.update {
            AttendanceUiState(
                currentStep = ShiftStep.START_DAY_INITIAL
            )
        }
    }
}
