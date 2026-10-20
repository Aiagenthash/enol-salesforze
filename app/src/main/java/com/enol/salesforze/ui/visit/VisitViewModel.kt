package com.enol.salesforze.ui.visit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import com.enol.salesforze.data.local.entities.VisitEntity
import com.enol.salesforze.data.remote.models.CheckInVisitRequest
import com.enol.salesforze.data.remote.models.CheckOutVisitRequest
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import com.enol.salesforze.ui.attendance.GpsLockValidator
import com.enol.salesforze.util.GeofenceUtil
import com.enol.salesforze.util.ImageWatermarkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

/**
 * Visit execution lifecycle states.
 */
enum class VisitStep {
    SELECT_RETAILER,
    GEOFENCE_CHECK,
    ACTIVE_VISIT,
    SELECT_OUTCOME,
    PAYMENT_RECOVERY,
    NO_ORDER_REASON,
    VISIT_SUMMARY
}

/**
 * UI State for Geofenced Visit Check-In/Out & Outcome Logging.
 */
data class VisitUiState(
    val currentStep: VisitStep = VisitStep.SELECT_RETAILER,
    val selectedClientId: String? = null,
    val selectedClient: ClientEntity? = null,
    val clients: List<ClientEntity> = emptyList(),
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAccuracyMeters: Float? = null,
    val isGpsLocked: Boolean = false,
    val isGpsAcquiring: Boolean = false,
    val geofenceDistanceMeters: Double? = null,
    val isWithinGeofence: Boolean = false,
    val forceCheckInReason: String = "",
    val activeVisit: VisitEntity? = null,
    val visitElapsedSeconds: Int = 0,
    val isMinDurationSatisfied: Boolean = false,
    val selectedOutcome: String = "NO_ORDER", // "ORDER_BOOKED", "PAYMENT_RECOVERY", "NO_ORDER"
    val noOrderReason: String = "Stock full",
    val recoveryAmountText: String = "",
    val recoverySlipNumber: String = "",
    val recoverySlipFile: File? = null,
    val shelfPhotoFile: File? = null,
    val isWatermarking: Boolean = false,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
) {
    val remainingSecondsToMinVisit: Int
        get() = (120 - visitElapsedSeconds).coerceAtLeast(0)

    val noOrderReasons = listOf(
        "Stock full",
        "Price issue / Competitor discount",
        "Owner absent / Shop closed",
        "Credit limit issue / Dispute",
        "Market liquidity issue"
    )
}

@HiltViewModel
class VisitViewModel @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val gpsLockValidator: GpsLockValidator,
    @ApplicationContext private val context: Context,
    private val json: Json,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialClientId: String? = savedStateHandle.get<String>("clientId")

    private val _uiState = MutableStateFlow(VisitUiState(selectedClientId = initialClientId))
    val uiState: StateFlow<VisitUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadClients()
    }

    private fun loadClients() {
        viewModelScope.launch {
            database.clientDao().getAllClients().collect { clientList ->
                _uiState.update { state ->
                    val chosen = if (!initialClientId.isNullOrBlank()) {
                        clientList.find { it.id == initialClientId }
                    } else {
                        state.selectedClient ?: clientList.firstOrNull()
                    }

                    state.copy(
                        clients = clientList,
                        selectedClient = chosen,
                        selectedClientId = chosen?.id,
                        currentStep = if (chosen != null) VisitStep.GEOFENCE_CHECK else VisitStep.SELECT_RETAILER
                    )
                }
                evaluateGeofence()
            }
        }
    }

    fun selectClient(client: ClientEntity) {
        _uiState.update {
            it.copy(
                selectedClientId = client.id,
                selectedClient = client,
                currentStep = VisitStep.GEOFENCE_CHECK
            )
        }
        evaluateGeofence()
    }

    /**
     * Acquires high-accuracy GPS satellite fix and validates distance against customer location.
     */
    fun acquireGpsAndCheckGeofence() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGpsAcquiring = true, errorMessage = null) }
            val result = gpsLockValidator.acquireValidGpsLock()
            result.onSuccess { loc ->
                _uiState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = true,
                        gpsLatitude = loc.latitude,
                        gpsLongitude = loc.longitude,
                        gpsAccuracyMeters = loc.accuracy
                    )
                }
                evaluateGeofence()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = false,
                        errorMessage = err.message ?: "Failed to acquire GPS fix."
                    )
                }
            }
        }
    }

    private fun evaluateGeofence() {
        val state = _uiState.value
        val client = state.selectedClient ?: return
        val userLat = state.gpsLatitude ?: return
        val userLng = state.gpsLongitude ?: return

        val distance = GeofenceUtil.calculateHaversineDistance(
            lat1 = userLat,
            lon1 = userLng,
            lat2 = client.latitude,
            lon2 = client.longitude
        )

        val isInside = distance <= GeofenceUtil.DEFAULT_VISIT_GEOFENCE_RADIUS_METERS

        _uiState.update {
            it.copy(
                geofenceDistanceMeters = distance,
                isWithinGeofence = isInside
            )
        }
    }

    fun setForceCheckInReason(reason: String) {
        _uiState.update { it.copy(forceCheckInReason = reason) }
    }

    /**
     * Executes visit check-in: verifies geofence radius or exception reason, persists to Room,
     * starts anti-cheat 2-minute minimum duration stopwatch, and enqueues sync payload.
     */
    fun performCheckIn(forceOverride: Boolean = false) {
        val state = _uiState.value
        val client = state.selectedClient

        if (client == null) {
            _uiState.update { it.copy(errorMessage = "Please select a client to check in.") }
            return
        }

        if (!state.isGpsLocked || state.gpsLatitude == null || state.gpsLongitude == null) {
            _uiState.update { it.copy(errorMessage = "GPS lock is required to verify on-site presence.") }
            return
        }

        if (!state.isWithinGeofence && !forceOverride) {
            _uiState.update {
                it.copy(
                    errorMessage = "You are outside the 100m geofence (${state.geofenceDistanceMeters?.toInt()}m away). Reason required to override."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val repId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
                    ?: throw IllegalStateException("Sales representative session not found.")

                val activeShift = database.attendanceDao().getActiveAttendanceForUserOnce(repId)
                val visitId = UUID.randomUUID().toString()
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val notesText = if (forceOverride) {
                    "FORCE_CHECK_IN_EXCEPTION: ${state.forceCheckInReason.ifBlank { "Location deviation" }} (Distance: ${state.geofenceDistanceMeters?.toInt()}m)"
                } else null

                val visitEntity = VisitEntity(
                    id = visitId,
                    attendanceId = activeShift?.id,
                    clientId = client.id,
                    salesRepId = repId,
                    scheduledAt = null,
                    checkInTime = nowIso,
                    checkOutTime = null,
                    checkInLatitude = state.gpsLatitude,
                    checkInLongitude = state.gpsLongitude,
                    checkOutLatitude = null,
                    checkOutLongitude = null,
                    geofenceDistanceMeters = state.geofenceDistanceMeters,
                    isMockGps = false,
                    outcome = "IN_PROGRESS",
                    notes = notesText,
                    recoverySlipNumber = null,
                    recoveryAmount = 0.0,
                    orderId = null,
                    updatedAt = nowIso
                )

                val checkInRequest = CheckInVisitRequest(
                    clientId = client.id,
                    attendanceId = activeShift?.id,
                    scheduledAt = null,
                    checkInLatitude = state.gpsLatitude,
                    checkInLongitude = state.gpsLongitude,
                    isMockGps = false,
                    outcome = "IN_PROGRESS",
                    notes = notesText
                )

                val syncQueue = PendingSyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "visit.check_in",
                    entityId = visitId,
                    action = "CHECK_IN_VISIT",
                    payloadJson = json.encodeToString(CheckInVisitRequest.serializer(), checkInRequest),
                    idempotencyKey = "checkin_${visitId}",
                    attemptCount = 0,
                    lastAttemptAt = null,
                    errorMessage = null,
                    createdAt = nowIso
                )

                withContext(Dispatchers.IO) {
                    database.visitDao().insertVisit(visitEntity)
                    database.pendingSyncQueueDao().insertSyncItem(syncQueue)
                }

                syncManager.triggerImmediateUploadSync()

                // Start Anti-Cheat 2-Minute Minimum Visit Stopwatch
                startVisitDurationTimer()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        activeVisit = visitEntity,
                        currentStep = VisitStep.ACTIVE_VISIT,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to check in.")
                }
            }
        }
    }

    private fun startVisitDurationTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var elapsed = 0
            while (true) {
                delay(1000L)
                elapsed++
                val minSatisfied = elapsed >= 120 // 2 minutes (120s)
                _uiState.update {
                    it.copy(
                        visitElapsedSeconds = elapsed,
                        isMinDurationSatisfied = minSatisfied
                    )
                }
            }
        }
    }

    fun setOutcome(outcome: String) {
        _uiState.update { it.copy(selectedOutcome = outcome) }
    }

    fun setNoOrderReason(reason: String) {
        _uiState.update { it.copy(noOrderReason = reason) }
    }

    fun setRecoveryFields(amount: String, slipNumber: String) {
        _uiState.update {
            it.copy(
                recoveryAmountText = amount,
                recoverySlipNumber = slipNumber
            )
        }
    }

    fun setNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun onRecoverySlipPhotoCaptured(uri: Uri) {
        val state = _uiState.value
        val lat = state.gpsLatitude ?: 0.0
        val lng = state.gpsLongitude ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isWatermarking = true) }
            try {
                val repId = sessionManager.getCachedUserId() ?: "UNKNOWN_REP"
                val watermarked = ImageWatermarkUtil.watermarkAndCompressImage(
                    context = context,
                    sourceUri = uri,
                    salesRepId = repId,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = state.gpsAccuracyMeters,
                    subDirectory = "recovery_slips"
                )
                _uiState.update {
                    it.copy(isWatermarking = false, recoverySlipFile = watermarked)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWatermarking = false, errorMessage = e.message) }
            }
        }
    }

    fun onShelfPhotoCaptured(uri: Uri) {
        val state = _uiState.value
        val lat = state.gpsLatitude ?: 0.0
        val lng = state.gpsLongitude ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isWatermarking = true) }
            try {
                val repId = sessionManager.getCachedUserId() ?: "UNKNOWN_REP"
                val watermarked = ImageWatermarkUtil.watermarkAndCompressImage(
                    context = context,
                    sourceUri = uri,
                    salesRepId = repId,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = state.gpsAccuracyMeters,
                    subDirectory = "shelf_audits"
                )
                _uiState.update {
                    it.copy(isWatermarking = false, shelfPhotoFile = watermarked)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWatermarking = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * Executes visit check-out: enforces 2-minute duration constraint, saves outcome
     * and recovery details, enqueues to sync worker, and stops timer.
     */
    fun performCheckOut(orderId: String? = null) {
        val state = _uiState.value
        val activeVisit = state.activeVisit

        if (activeVisit == null) {
            _uiState.update { it.copy(errorMessage = "No active visit to check out.") }
            return
        }

        if (!state.isMinDurationSatisfied) {
            _uiState.update {
                it.copy(
                    errorMessage = "Minimum 2-minute visit inspection duration required (${state.remainingSecondsToMinVisit}s remaining)."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                // Acquire fresh check-out GPS
                val gpsResult = gpsLockValidator.acquireValidGpsLock()
                val loc = gpsResult.getOrNull()
                val outLat = loc?.latitude ?: activeVisit.checkInLatitude
                val outLng = loc?.longitude ?: activeVisit.checkInLongitude

                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val recoveryAmount = state.recoveryAmountText.toDoubleOrNull() ?: 0.0
                val recoverySlip = state.recoverySlipNumber.ifBlank { null }

                val finalNotes = when (state.selectedOutcome) {
                    "NO_ORDER" -> "Reason: ${state.noOrderReason}. ${state.notes}".trim()
                    "PAYMENT_RECOVERY" -> "Recovered PKR %,.0f. Slip: %s. %s".format(recoveryAmount, recoverySlip ?: "N/A", state.notes).trim()
                    else -> state.notes.ifBlank { null }
                }

                withContext(Dispatchers.IO) {
                    database.visitDao().checkOutVisit(
                        visitId = activeVisit.id,
                        checkOutTime = nowIso,
                        checkOutLat = outLat,
                        checkOutLng = outLng,
                        outcome = state.selectedOutcome,
                        notes = finalNotes,
                        slipNumber = recoverySlip,
                        recoveryAmount = recoveryAmount,
                        orderId = orderId,
                        updatedAt = nowIso
                    )

                    val checkOutRequest = CheckOutVisitRequest(
                        checkOutLatitude = outLat,
                        checkOutLongitude = outLng,
                        outcome = state.selectedOutcome,
                        notes = finalNotes,
                        recoverySlipNumber = recoverySlip,
                        recoveryAmount = recoveryAmount,
                        orderId = orderId
                    )

                    val syncQueue = PendingSyncQueueEntity(
                        id = UUID.randomUUID().toString(),
                        entityType = "visit.check_out",
                        entityId = activeVisit.id,
                        action = "CHECK_OUT_VISIT",
                        payloadJson = json.encodeToString(CheckOutVisitRequest.serializer(), checkOutRequest),
                        idempotencyKey = "checkout_${activeVisit.id}",
                        attemptCount = 0,
                        lastAttemptAt = null,
                        errorMessage = null,
                        createdAt = nowIso
                    )

                    database.pendingSyncQueueDao().insertSyncItem(syncQueue)
                }

                timerJob?.cancel()
                syncManager.triggerImmediateUploadSync()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isCompleted = true,
                        currentStep = VisitStep.VISIT_SUMMARY,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to check out.")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
