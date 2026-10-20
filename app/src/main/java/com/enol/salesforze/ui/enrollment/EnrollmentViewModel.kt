package com.enol.salesforze.ui.enrollment

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.BeatEntity
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.DistrictEntity
import com.enol.salesforze.data.local.entities.MechanicEntity
import com.enol.salesforze.data.local.entities.PendingSyncQueueEntity
import com.enol.salesforze.data.remote.models.CreateClientRequest
import com.enol.salesforze.data.remote.models.RegisterMechanicRequest
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import com.enol.salesforze.ui.attendance.GpsLockValidator
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

/**
 * UI State for Client (Dealer / Retailer) Onboarding.
 */
data class ClientEnrollmentUiState(
    val businessName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val cnic: String = "",
    val address: String = "",
    val selectedDistrictId: String? = null,
    val selectedBeatId: String? = null,
    val creditLimitText: String = "100000",
    val rawPhotoUri: Uri? = null,
    val watermarkedPhotoFile: File? = null,
    val isWatermarking: Boolean = false,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAccuracyMeters: Float? = null,
    val isGpsLocked: Boolean = false,
    val isGpsAcquiring: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    val createdClientId: String? = null,
    val districts: List<DistrictEntity> = emptyList(),
    val beats: List<BeatEntity> = emptyList(),
    val errorMessage: String? = null
)

/**
 * UI State for Mechanic (Loyalty Program) Onboarding.
 */
data class MechanicEnrollmentUiState(
    val name: String = "",
    val phone: String = "",
    val cnic: String = "",
    val specialty: String = "4-STROKE_MOTORCYCLE",
    val walletProvider: String = "JAZZCASH",
    val walletAccountNumber: String = "",
    val selectedClientId: String? = null,
    val rawPhotoUri: Uri? = null,
    val watermarkedPhotoFile: File? = null,
    val isWatermarking: Boolean = false,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAccuracyMeters: Float? = null,
    val isGpsLocked: Boolean = false,
    val isGpsAcquiring: Boolean = false,
    val isSubmitting: Boolean = false,
    val submissionSuccess: Boolean = false,
    val createdMechanicId: String? = null,
    val clients: List<ClientEntity> = emptyList(),
    val errorMessage: String? = null
) {
    val specialties = listOf(
        "4-STROKE_MOTORCYCLE",
        "HEAVY_DIESEL",
        "PASSENGER_CAR",
        "AGRICULTURE_TRACTOR"
    )
    val walletProviders = listOf(
        "JAZZCASH",
        "EASYPAISA",
        "SADAPAY",
        "NAYAPAY"
    )
}

@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val gpsLockValidator: GpsLockValidator,
    @ApplicationContext private val context: Context,
    private val json: Json
) : ViewModel() {

    private val _clientState = MutableStateFlow(ClientEnrollmentUiState())
    val clientState: StateFlow<ClientEnrollmentUiState> = _clientState.asStateFlow()

    private val _mechanicState = MutableStateFlow(MechanicEnrollmentUiState())
    val mechanicState: StateFlow<MechanicEnrollmentUiState> = _mechanicState.asStateFlow()

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            database.districtDao().getAllDistricts().collect { districtList ->
                _clientState.update { state ->
                    state.copy(
                        districts = districtList,
                        selectedDistrictId = state.selectedDistrictId ?: districtList.firstOrNull()?.id
                    )
                }
            }
        }

        viewModelScope.launch {
            database.beatDao().getAllBeats().collect { beatList ->
                _clientState.update { it.copy(beats = beatList) }
            }
        }

        viewModelScope.launch {
            database.clientDao().getAllClients().collect { clientList ->
                _mechanicState.update { state ->
                    state.copy(
                        clients = clientList,
                        selectedClientId = state.selectedClientId ?: clientList.firstOrNull()?.id
                    )
                }
            }
        }
    }

    // =========================================================================
    // CLIENT ENROLLMENT ACTIONS
    // =========================================================================

    fun onClientFieldChanged(
        businessName: String? = null,
        ownerName: String? = null,
        phone: String? = null,
        cnic: String? = null,
        address: String? = null,
        districtId: String? = null,
        beatId: String? = null,
        creditLimit: String? = null
    ) {
        _clientState.update { state ->
            state.copy(
                businessName = businessName ?: state.businessName,
                ownerName = ownerName ?: state.ownerName,
                phone = phone ?: state.phone,
                cnic = cnic ?: state.cnic,
                address = address ?: state.address,
                selectedDistrictId = districtId ?: state.selectedDistrictId,
                selectedBeatId = beatId ?: state.selectedBeatId,
                creditLimitText = creditLimit ?: state.creditLimitText,
                errorMessage = null
            )
        }
    }

    fun acquireClientGpsLock() {
        viewModelScope.launch {
            _clientState.update { it.copy(isGpsAcquiring = true, errorMessage = null) }
            val result = gpsLockValidator.acquireValidGpsLock()
            result.onSuccess { loc ->
                _clientState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = true,
                        gpsLatitude = loc.latitude,
                        gpsLongitude = loc.longitude,
                        gpsAccuracyMeters = loc.accuracy
                    )
                }
            }.onFailure { err ->
                _clientState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = false,
                        errorMessage = err.message ?: "Failed to acquire GPS lock"
                    )
                }
            }
        }
    }

    fun onClientStorefrontPhotoCaptured(uri: Uri) {
        val state = _clientState.value
        val lat = state.gpsLatitude
        val lng = state.gpsLongitude

        if (!state.isGpsLocked || lat == null || lng == null) {
            _clientState.update { it.copy(errorMessage = "Acquire valid GPS lock before capturing storefront photo.") }
            return
        }

        viewModelScope.launch {
            _clientState.update { it.copy(isWatermarking = true, rawPhotoUri = uri) }
            try {
                val repId = sessionManager.getCachedUserId() ?: "UNKNOWN_REP"
                val watermarkedFile = ImageWatermarkUtil.watermarkAndCompressImage(
                    context = context,
                    sourceUri = uri,
                    salesRepId = repId,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = state.gpsAccuracyMeters,
                    subDirectory = "storefront_photos"
                )
                _clientState.update {
                    it.copy(
                        isWatermarking = false,
                        watermarkedPhotoFile = watermarkedFile,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _clientState.update {
                    it.copy(
                        isWatermarking = false,
                        errorMessage = "Failed to watermark photo: ${e.message}"
                    )
                }
            }
        }
    }

    fun submitClientEnrollment() {
        val state = _clientState.value

        if (state.businessName.isBlank()) {
            _clientState.update { it.copy(errorMessage = "Business/Shop name is required.") }
            return
        }
        if (state.ownerName.isBlank()) {
            _clientState.update { it.copy(errorMessage = "Owner name is required.") }
            return
        }
        if (state.phone.isBlank()) {
            _clientState.update { it.copy(errorMessage = "Phone number is required.") }
            return
        }
        if (state.address.isBlank()) {
            _clientState.update { it.copy(errorMessage = "Storefront address is required.") }
            return
        }
        if (state.selectedDistrictId.isNullOrBlank()) {
            _clientState.update { it.copy(errorMessage = "Please assign a District.") }
            return
        }
        if (!state.isGpsLocked || state.gpsLatitude == null || state.gpsLongitude == null) {
            _clientState.update { it.copy(errorMessage = "Valid on-site GPS lock (<= 25m) is mandatory.") }
            return
        }
        if (state.watermarkedPhotoFile == null) {
            _clientState.update { it.copy(errorMessage = "Storefront photo capture with forensic watermark is required.") }
            return
        }

        viewModelScope.launch {
            _clientState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val clientId = UUID.randomUUID().toString()
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val creditLimit = state.creditLimitText.toDoubleOrNull() ?: 100000.0

                val clientEntity = ClientEntity(
                    id = clientId,
                    beatId = state.selectedBeatId,
                    districtId = state.selectedDistrictId,
                    businessName = state.businessName.trim(),
                    ownerName = state.ownerName.trim(),
                    phone = state.phone.trim(),
                    cnic = state.cnic.trim().ifBlank { null },
                    address = state.address.trim(),
                    latitude = state.gpsLatitude,
                    longitude = state.gpsLongitude,
                    creditLimit = creditLimit,
                    outstandingBalance = 0.0,
                    creditHold = false,
                    approvalStatus = "PENDING_APPROVAL",
                    updatedAt = nowIso
                )

                val createRequest = CreateClientRequest(
                    districtId = state.selectedDistrictId,
                    beatId = state.selectedBeatId,
                    businessName = state.businessName.trim(),
                    ownerName = state.ownerName.trim(),
                    phone = state.phone.trim(),
                    cnic = state.cnic.trim().ifBlank { null },
                    address = state.address.trim(),
                    latitude = state.gpsLatitude,
                    longitude = state.gpsLongitude,
                    creditLimit = creditLimit
                )

                val syncQueue = PendingSyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "client.create",
                    entityId = clientId,
                    action = "CREATE_CLIENT",
                    payloadJson = json.encodeToString(CreateClientRequest.serializer(), createRequest),
                    idempotencyKey = "client_${clientId}_${System.currentTimeMillis()}",
                    attemptCount = 0,
                    lastAttemptAt = null,
                    errorMessage = null,
                    createdAt = nowIso
                )

                withContext(Dispatchers.IO) {
                    database.clientDao().insertClient(clientEntity)
                    database.pendingSyncQueueDao().insertSyncItem(syncQueue)
                }

                syncManager.triggerImmediateUploadSync()

                _clientState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        createdClientId = clientId
                    )
                }
            } catch (e: Exception) {
                _clientState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to save client.")
                }
            }
        }
    }

    // =========================================================================
    // MECHANIC ENROLLMENT ACTIONS
    // =========================================================================

    fun onMechanicFieldChanged(
        name: String? = null,
        phone: String? = null,
        cnic: String? = null,
        specialty: String? = null,
        walletProvider: String? = null,
        walletAccountNumber: String? = null,
        clientId: String? = null
    ) {
        _mechanicState.update { state ->
            state.copy(
                name = name ?: state.name,
                phone = phone ?: state.phone,
                cnic = cnic ?: state.cnic,
                specialty = specialty ?: state.specialty,
                walletProvider = walletProvider ?: state.walletProvider,
                walletAccountNumber = walletAccountNumber ?: state.walletAccountNumber,
                selectedClientId = clientId ?: state.selectedClientId,
                errorMessage = null
            )
        }
    }

    fun acquireMechanicGpsLock() {
        viewModelScope.launch {
            _mechanicState.update { it.copy(isGpsAcquiring = true, errorMessage = null) }
            val result = gpsLockValidator.acquireValidGpsLock()
            result.onSuccess { loc ->
                _mechanicState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = true,
                        gpsLatitude = loc.latitude,
                        gpsLongitude = loc.longitude,
                        gpsAccuracyMeters = loc.accuracy
                    )
                }
            }.onFailure { err ->
                _mechanicState.update {
                    it.copy(
                        isGpsAcquiring = false,
                        isGpsLocked = false,
                        errorMessage = err.message ?: "Failed to acquire GPS lock"
                    )
                }
            }
        }
    }

    fun onMechanicWorkshopPhotoCaptured(uri: Uri) {
        val state = _mechanicState.value
        val lat = state.gpsLatitude
        val lng = state.gpsLongitude

        if (!state.isGpsLocked || lat == null || lng == null) {
            _mechanicState.update { it.copy(errorMessage = "Acquire valid GPS lock before capturing workshop photo.") }
            return
        }

        viewModelScope.launch {
            _mechanicState.update { it.copy(isWatermarking = true, rawPhotoUri = uri) }
            try {
                val repId = sessionManager.getCachedUserId() ?: "UNKNOWN_REP"
                val watermarkedFile = ImageWatermarkUtil.watermarkAndCompressImage(
                    context = context,
                    sourceUri = uri,
                    salesRepId = repId,
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = state.gpsAccuracyMeters,
                    subDirectory = "mechanic_photos"
                )
                _mechanicState.update {
                    it.copy(
                        isWatermarking = false,
                        watermarkedPhotoFile = watermarkedFile,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _mechanicState.update {
                    it.copy(
                        isWatermarking = false,
                        errorMessage = "Failed to watermark photo: ${e.message}"
                    )
                }
            }
        }
    }

    fun submitMechanicEnrollment() {
        val state = _mechanicState.value

        if (state.name.isBlank()) {
            _mechanicState.update { it.copy(errorMessage = "Mechanic name is required.") }
            return
        }
        if (state.phone.isBlank()) {
            _mechanicState.update { it.copy(errorMessage = "Phone number is required.") }
            return
        }
        if (state.walletAccountNumber.isBlank()) {
            _mechanicState.update { it.copy(errorMessage = "Wallet account number is required.") }
            return
        }
        if (!state.isGpsLocked || state.gpsLatitude == null || state.gpsLongitude == null) {
            _mechanicState.update { it.copy(errorMessage = "GPS lock is mandatory to verify workshop locality.") }
            return
        }
        if (state.watermarkedPhotoFile == null) {
            _mechanicState.update { it.copy(errorMessage = "Workshop photo capture is required.") }
            return
        }

        viewModelScope.launch {
            _mechanicState.update { it.copy(isSubmitting = true, errorMessage = null) }

            try {
                val mechanicId = UUID.randomUUID().toString()
                val qrCodeId = "ENOL-MECH-${UUID.randomUUID().toString().take(8).uppercase()}"
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())

                val mechanicEntity = MechanicEntity(
                    id = mechanicId,
                    clientId = state.selectedClientId,
                    name = state.name.trim(),
                    phone = state.phone.trim(),
                    cnic = state.cnic.trim().ifBlank { null },
                    specialty = state.specialty,
                    walletProvider = state.walletProvider,
                    walletAccountNumber = state.walletAccountNumber.trim(),
                    qrCodeId = qrCodeId,
                    pointsBalance = 0,
                    totalScans = 0,
                    isVerified = true,
                    updatedAt = nowIso
                )

                val registerRequest = RegisterMechanicRequest(
                    clientId = state.selectedClientId,
                    name = state.name.trim(),
                    phone = state.phone.trim(),
                    cnic = state.cnic.trim().ifBlank { null },
                    specialty = state.specialty,
                    walletProvider = state.walletProvider,
                    walletAccountNumber = state.walletAccountNumber.trim(),
                    qrCodeId = qrCodeId
                )

                val syncQueue = PendingSyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "mechanic.register",
                    entityId = mechanicId,
                    action = "REGISTER_MECHANIC",
                    payloadJson = json.encodeToString(RegisterMechanicRequest.serializer(), registerRequest),
                    idempotencyKey = "mech_${mechanicId}_${System.currentTimeMillis()}",
                    attemptCount = 0,
                    lastAttemptAt = null,
                    errorMessage = null,
                    createdAt = nowIso
                )

                withContext(Dispatchers.IO) {
                    database.mechanicDao().insertMechanic(mechanicEntity)
                    database.pendingSyncQueueDao().insertSyncItem(syncQueue)
                }

                syncManager.triggerImmediateUploadSync()

                _mechanicState.update {
                    it.copy(
                        isSubmitting = false,
                        submissionSuccess = true,
                        createdMechanicId = mechanicId
                    )
                }
            } catch (e: Exception) {
                _mechanicState.update {
                    it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to save mechanic.")
                }
            }
        }
    }

    fun resetClientForm() {
        _clientState.update { ClientEnrollmentUiState() }
        loadMetadata()
    }

    fun resetMechanicForm() {
        _mechanicState.update { MechanicEnrollmentUiState() }
        loadMetadata()
    }
}
