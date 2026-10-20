package com.enol.salesforze.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.CompetitionEntity
import com.enol.salesforze.data.local.entities.DistrictEntity
import com.enol.salesforze.data.local.entities.OrderEntity
import com.enol.salesforze.data.local.entities.TadaClaimEntity
import com.enol.salesforze.data.local.entities.TargetKpiEntity
import com.enol.salesforze.data.local.entities.UserEntity
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * 5-Tier Organizational Hierarchy Roles.
 */
enum class UserRole {
    DIRECTOR,
    ZSM,
    ASM,
    SALES_REP,
    ADMIN;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()) {
                "DIRECTOR" -> DIRECTOR
                "ZSM" -> ZSM
                "ASM" -> ASM
                "ADMIN" -> ADMIN
                else -> SALES_REP
            }
        }
    }
}

/**
 * Aggregated live team operation counts for Managers & Directors.
 */
data class TeamStatusTicker(
    val onFieldCount: Int = 18,
    val notStartedCount: Int = 4,
    val mockGpsFlaggedCount: Int = 1,
    val shiftEndedCount: Int = 7
)

/**
 * UI State for the Role-Based Hierarchy Dashboard & Executive Command Center.
 */
data class DashboardUiState(
    val userRole: UserRole = UserRole.SALES_REP,
    val currentUser: UserEntity? = null,
    // Sales Rep Metrics
    val todayVisitsCompleted: Int = 6,
    val todayVisitsPlanned: Int = 10,
    val targetVolumeLiters: Double = 5000.0,
    val achievedVolumeLiters: Double = 3720.0,
    val targetRevenuePkr: Double = 2500000.0,
    val achievedRevenuePkr: Double = 1860000.0,
    val targetVisits: Int = 180,
    val achievedVisits: Int = 138,
    val targetNewClients: Int = 15,
    val achievedNewClients: Int = 11,
    val activeCompetitions: List<CompetitionEntity> = emptyList(),
    val recentTadaClaims: List<TadaClaimEntity> = emptyList(),
    // Manager & Executive Metrics
    val teamTicker: TeamStatusTicker = TeamStatusTicker(),
    val selectedHierarchyFilter: String = "National",
    val districts: List<DistrictEntity> = emptyList(),
    val pendingCreditHoldOrders: List<OrderEntity> = emptyList(),
    val pendingEnrollmentClients: List<ClientEntity> = emptyList(),
    val pendingTadaClaims: List<TadaClaimEntity> = emptyList(),
    val isApprovalCenterOpen: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isManagerOrDirector: Boolean
        get() = userRole == UserRole.DIRECTOR || userRole == UserRole.ZSM || userRole == UserRole.ASM || userRole == UserRole.ADMIN

    val volumeProgressPct: Float
        get() = if (targetVolumeLiters > 0) (achievedVolumeLiters / targetVolumeLiters).toFloat().coerceIn(0f, 1f) else 0f

    val revenueProgressPct: Float
        get() = if (targetRevenuePkr > 0) (achievedRevenuePkr / targetRevenuePkr).toFloat().coerceIn(0f, 1f) else 0f

    val beatProgressPct: Float
        get() = if (todayVisitsPlanned > 0) (todayVisitsCompleted.toFloat() / todayVisitsPlanned).coerceIn(0f, 1f) else 0f

    val totalPendingApprovals: Int
        get() = pendingCreditHoldOrders.size + pendingEnrollmentClients.size + pendingTadaClaims.size
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = sessionManager.getCachedUserId() ?: sessionManager.getUserId()
            if (userId != null) {
                val user = database.userDao().getUserByIdOnce(userId)
                val role = UserRole.fromString(user?.role)

                _uiState.update {
                    it.copy(
                        currentUser = user,
                        userRole = role
                    )
                }

                // Load Rep-specific Data
                database.competitionDao().getActiveCompetitions().collect { comps ->
                    _uiState.update { it.copy(activeCompetitions = comps) }
                }

                database.tadaClaimDao().getClaimsByUser(userId).collect { claims ->
                    _uiState.update { it.copy(recentTadaClaims = claims) }
                }

                // If Manager or Director, load Approval Center queues & Team Ticker
                if (role != UserRole.SALES_REP) {
                    loadManagerQueues()
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadManagerQueues() {
        viewModelScope.launch {
            database.orderDao().getOrdersByStatus("CREDIT_HOLD").collect { creditOrders ->
                _uiState.update { it.copy(pendingCreditHoldOrders = creditOrders) }
            }
        }

        viewModelScope.launch {
            database.clientDao().getAllClients().collect { allClients ->
                val pendingClients = allClients.filter { it.approvalStatus == "PENDING_APPROVAL" }
                _uiState.update { it.copy(pendingEnrollmentClients = pendingClients) }
            }
        }

        viewModelScope.launch {
            database.tadaClaimDao().getClaimsByStatus("SUBMITTED").collect { submitted ->
                val flagged = database.tadaClaimDao().getClaimsByStatus("FLAGGED")
                flagged.collect { flaggedList ->
                    _uiState.update { it.copy(pendingTadaClaims = submitted + flaggedList) }
                }
            }
        }

        viewModelScope.launch {
            database.districtDao().getAllDistricts().collect { dists ->
                _uiState.update { it.copy(districts = dists) }
            }
        }
    }

    fun setHierarchyFilter(filter: String) {
        _uiState.update { it.copy(selectedHierarchyFilter = filter) }
    }

    fun toggleApprovalCenter(open: Boolean) {
        _uiState.update { it.copy(isApprovalCenterOpen = open) }
    }

    // =========================================================================
    // ONE-CLICK APPROVAL ACTIONS
    // =========================================================================

    fun approveCreditHoldOrder(orderId: String) {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            withContext(Dispatchers.IO) {
                database.orderDao().updateOrderStatus(orderId, "APPROVED", nowIso)
            }
            syncManager.triggerImmediateUploadSync()
            loadManagerQueues()
        }
    }

    fun rejectCreditHoldOrder(orderId: String) {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            withContext(Dispatchers.IO) {
                database.orderDao().updateOrderStatus(orderId, "REJECTED", nowIso)
            }
            syncManager.triggerImmediateUploadSync()
            loadManagerQueues()
        }
    }

    fun approveClientEnrollment(clientId: String) {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            withContext(Dispatchers.IO) {
                database.clientDao().updateClientApprovalStatus(clientId, "APPROVED", nowIso)
            }
            syncManager.triggerImmediateUploadSync()
            loadManagerQueues()
        }
    }

    fun rejectClientEnrollment(clientId: String) {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            withContext(Dispatchers.IO) {
                database.clientDao().updateClientApprovalStatus(clientId, "REJECTED", nowIso)
            }
            syncManager.triggerImmediateUploadSync()
            loadManagerQueues()
        }
    }

    fun approveTadaClaim(claimId: String) {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            withContext(Dispatchers.IO) {
                database.tadaClaimDao().updateClaimStatus(claimId, "APPROVED", null, nowIso)
            }
            syncManager.triggerImmediateUploadSync()
            loadManagerQueues()
        }
    }

    fun rejectTadaClaim(claimId: String, reason: String = "Audit Discrepancy") {
        viewModelScope.launch {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            withContext(Dispatchers.IO) {
                database.tadaClaimDao().updateClaimStatus(claimId, "REJECTED", reason, nowIso)
            }
            syncManager.triggerImmediateUploadSync()
            loadManagerQueues()
        }
    }
}
