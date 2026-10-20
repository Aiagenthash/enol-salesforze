package com.enol.salesforze.ui.tada

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.AttendanceEntity
import com.enol.salesforze.data.local.entities.BreadcrumbEntity
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import com.enol.salesforze.data.local.entities.TadaClaimEntity
import com.enol.salesforze.data.remote.models.SubmitTadaClaimRequest
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import com.enol.salesforze.util.GeofenceUtil
import com.enol.salesforze.util.ImageWatermarkUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
import kotlin.math.abs

/**
 * UI State for TADA Travel Allowance & Odometer Verification Screen.
 */
data class TadaUiState(
    val activeAttendance: AttendanceEntity? = null,
    val claimDate: String = "",
    val startOdometer: Double = 0.0,
    val endOdometerText: String = "",
    val closingOdometerPhotoUri: Uri? = null,
    val watermarkedOdometerFile: File? = null,
    val hotelBillPhotoUri: Uri? = null,
    val watermarkedHotelBillFile: File? = null,
    val isWatermarking: Boolean = false,
    val gpsCalculatedKm: Double = 0.0,
    val odometerDeltaKm: Double = 0.0,
    val variancePercentage: Double = 0.0,
    val isDiscrepancyFlagged: Boolean = false,
    val isOutstationNightStay: Boolean = false,
    val fuelRatePerKm: Double = 7.50,
    val localDailyAllowance: Double = 500.0,
    val outstationNightAllowance: Double = 2500.0,
    val fuelClaimAmount: Double = 0.0,
    val dailyAllowanceAmount: Double = 500.0,
    val tollParkingText: String = "0",
    val totalClaimAmount: Double = 500.0,
    val claimStatus: String = "SUBMITTED",
    val userClaims: List<TadaClaimEntity> = emptyList(),
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    val createdClaimId: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class TadaViewModel @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow(TadaUiState())
    val uiState: StateFlow<TadaUiState> = _uiState.asStateFlow()

    init {
        loadActiveShiftAndHistory()
    }

    private fun loadActiveShiftAndHistory() {
        viewModelScope.launch {
            val userId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
            if (userId != null) {
                // Load user rates from UserEntity
                val user = database.userDao().getUserByIdOnce(userId)
                val fuelRate = user?.fuelRatePerKm ?: 7.50
                val dailyAllowance = user?.dailyAllowance ?: 500.0

                _uiState.update {
                    it.copy(
                        fuelRatePerKm = fuelRate,
                        localDailyAllowance = dailyAllowance,
                        dailyAllowanceAmount = dailyAllowance
                    )
                }

                // Load active or latest attendance for today
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val todayAttendance = database.attendanceDao().getAttendanceByUserAndDateOnce(userId, todayDate)
                    ?: database.attendanceDao().getActiveAttendanceForUserOnce(userId)

                if (todayAttendance != null) {
                    val startOdo = todayAttendance.startOdometerReading
                    val endOdo = todayAttendance.endOdometerReading ?: startOdo

                    // Calculate GPS distance from breadcrumbs
                    val breadcrumbs = database.breadcrumbDao().getBreadcrumbsByAttendanceOnce(todayAttendance.id)
                    val gpsKm = calculateGpsDistanceKm(breadcrumbs)

                    _uiState.update {
                        it.copy(
                            activeAttendance = todayAttendance,
                            claimDate = todayAttendance.date,
                            startOdometer = startOdo,
                            endOdometerText = if (endOdo > startOdo) endOdo.toInt().toString() else "",
                            gpsCalculatedKm = gpsKm
                        )
                    }
                    recalculateDiscrepancyAndTotals()
                }

                // Load claims history
                database.tadaClaimDao().getClaimsByUser(userId).collect { claims ->
                    _uiState.update { it.copy(userClaims = claims) }
                }
            }
        }
    }

    private fun calculateGpsDistanceKm(breadcrumbs: List<BreadcrumbEntity>): Double {
        if (breadcrumbs.size < 2) return 0.0
        var totalMeters = 0.0
        for (i in 0 until breadcrumbs.size - 1) {
            val p1 = breadcrumbs[i]
            val p2 = breadcrumbs[i + 1]
            totalMeters += GeofenceUtil.calculateHaversineDistance(
                lat1 = p1.latitude,
                lon1 = p1.longitude,
                lat2 = p2.latitude,
                lon2 = p2.longitude
            )
        }
        return Math.round((totalMeters / 1000.0) * 10.0) / 10.0
    }

    fun onEndOdometerChanged(text: String) {
        _uiState.update { it.copy(endOdometerText = text) }
        recalculateDiscrepancyAndTotals()
    }

    fun onTollParkingChanged(text: String) {
        _uiState.update { it.copy(tollParkingText = text) }
        recalculateDiscrepancyAndTotals()
    }

    fun toggleOutstationNightStay(isOutstation: Boolean) {
        _uiState.update { it.copy(isOutstationNightStay = isOutstation) }
        recalculateDiscrepancyAndTotals()
    }

    fun onClosingPhotoCaptured(uri: Uri) {
        val state = _uiState.value
        val lat = state.activeAttendance?.startLatitude ?: 0.0
        val lng = state.activeAttendance?.startLongitude ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isWatermarking = true, closingOdometerPhotoUri = uri) }
            try {
                val repId = sessionManager.getCachedUserId() ?: "UNKNOWN_REP"
                val watermarked = ImageWatermarkUtil.watermarkAndCompressImage(
                    context = context,
                    sourceUri = uri,
                    salesRepId = repId,
                    latitude = lat,
                    longitude = lng,
                    subDirectory = "tada_odometer"
                )
                _uiState.update { it.copy(isWatermarking = false, watermarkedOdometerFile = watermarked) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWatermarking = false, errorMessage = e.message) }
            }
        }
    }

    fun onHotelBillPhotoCaptured(uri: Uri) {
        val state = _uiState.value
        val lat = state.activeAttendance?.startLatitude ?: 0.0
        val lng = state.activeAttendance?.startLongitude ?: 0.0

        viewModelScope.launch {
            _uiState.update { it.copy(isWatermarking = true, hotelBillPhotoUri = uri) }
            try {
                val repId = sessionManager.getCachedUserId() ?: "UNKNOWN_REP"
                val watermarked = ImageWatermarkUtil.watermarkAndCompressImage(
                    context = context,
                    sourceUri = uri,
                    salesRepId = repId,
                    latitude = lat,
                    longitude = lng,
                    subDirectory = "hotel_bills"
                )
                _uiState.update { it.copy(isWatermarking = false, watermarkedHotelBillFile = watermarked) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWatermarking = false, errorMessage = e.message) }
            }
        }
    }

    private fun recalculateDiscrepancyAndTotals() {
        val state = _uiState.value
        val endOdo = state.endOdometerText.toDoubleOrNull() ?: state.startOdometer
        val odoDelta = (endOdo - state.startOdometer).coerceAtLeast(0.0)

        val gpsKm = state.gpsCalculatedKm
        val variancePct = if (gpsKm > 0.5) {
            Math.round((abs(odoDelta - gpsKm) / gpsKm * 100.0) * 10.0) / 10.0
        } else {
            0.0
        }

        // Strict 10% discrepancy guardrail
        val isFlagged = variancePct > 10.0 && odoDelta > 5.0

        val fuelClaim = Math.round(odoDelta * state.fuelRatePerKm * 10.0) / 10.0
        val dailyAllowance = if (state.isOutstationNightStay) state.outstationNightAllowance else state.localDailyAllowance
        val tollParking = state.tollParkingText.toDoubleOrNull() ?: 0.0
        val totalClaim = fuelClaim + dailyAllowance + tollParking

        _uiState.update {
            it.copy(
                odometerDeltaKm = odoDelta,
                variancePercentage = variancePct,
                isDiscrepancyFlagged = isFlagged,
                fuelClaimAmount = fuelClaim,
                dailyAllowanceAmount = dailyAllowance,
                totalClaimAmount = totalClaim,
                claimStatus = if (isFlagged) "FLAGGED" else "SUBMITTED"
            )
        }
    }

    /**
     * Submits the daily TADA expense claim to Room DB and enqueues to PendingSyncQueue for approval.
     */
    fun submitTadaClaim() {
        val state = _uiState.value
        val attendance = state.activeAttendance

        if (attendance == null) {
            _uiState.update { it.copy(errorMessage = "No active field attendance found for claim generation.") }
            return
        }

        val endOdo = state.endOdometerText.toDoubleOrNull()
        if (endOdo == null || endOdo < state.startOdometer) {
            _uiState.update { it.copy(errorMessage = "Closing odometer reading must be >= starting odometer (${state.startOdometer} km).") }
            return
        }

        if (state.watermarkedOdometerFile == null) {
            _uiState.update { it.copy(errorMessage = "Closing odometer photo with verified watermark is required.") }
            return
        }

        if (state.isOutstationNightStay && state.watermarkedHotelBillFile == null) {
            _uiState.update { it.copy(errorMessage = "Hotel bill photo upload is required for Outstation / Night Stay claims.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val userId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
                    ?: throw IllegalStateException("User session not found.")

                val claimId = UUID.randomUUID().toString()
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val tollAmount = state.tollParkingText.toDoubleOrNull() ?: 0.0

                val claimEntity = TadaClaimEntity(
                    id = claimId,
                    attendanceId = attendance.id,
                    userId = userId,
                    claimDate = state.claimDate,
                    startOdometer = state.startOdometer,
                    endOdometer = endOdo,
                    odometerDeltaKm = state.odometerDeltaKm,
                    gpsDistanceKm = state.gpsCalculatedKm,
                    variancePercentage = state.variancePercentage,
                    fuelRatePerKm = state.fuelRatePerKm,
                    fuelClaimAmount = state.fuelClaimAmount,
                    dailyAllowanceAmount = state.dailyAllowanceAmount,
                    tollParkingAmount = tollAmount,
                    totalClaimAmount = state.totalClaimAmount,
                    status = state.claimStatus,
                    rejectionReason = if (state.isDiscrepancyFlagged) "Distance variance ${state.variancePercentage}% exceeds 10% threshold" else null,
                    updatedAt = nowIso
                )

                val submitRequest = SubmitTadaClaimRequest(
                    attendanceId = attendance.id,
                    claimDate = state.claimDate,
                    startOdometer = state.startOdometer,
                    endOdometer = endOdo,
                    gpsDistanceKm = state.gpsCalculatedKm,
                    tollParkingAmount = tollAmount
                )

                val syncQueue = PendingSyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "tada.submit",
                    entityId = claimId,
                    action = "SUBMIT_TADA_CLAIM",
                    payloadJson = json.encodeToString(SubmitTadaClaimRequest.serializer(), submitRequest),
                    idempotencyKey = "tada_${claimId}_${System.currentTimeMillis()}",
                    attemptCount = 0,
                    lastAttemptAt = null,
                    errorMessage = null,
                    createdAt = nowIso
                )

                withContext(Dispatchers.IO) {
                    database.tadaClaimDao().insertClaim(claimEntity)
                    database.pendingSyncQueueDao().insertSyncItem(syncQueue)
                }

                syncManager.triggerImmediateUploadSync()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        createdClaimId = claimId,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to submit claim.")
                }
            }
        }
    }

    fun resetForm() {
        _uiState.update {
            it.copy(
                submissionSuccess = false,
                createdClaimId = null,
                errorMessage = null
            )
        }
        loadActiveShiftAndHistory()
    }
}
