package com.enol.salesforze.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.UserEntity
import com.enol.salesforze.data.remote.api.EnterpriseFieldApi
import com.enol.salesforze.data.remote.models.LoginRequest
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val loggedInRole: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: EnterpriseFieldApi,
    private val sessionManager: SessionManager,
    private val database: AppDatabase,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = sessionManager.getCachedAuthToken() ?: sessionManager.getAuthToken()
            val role = sessionManager.userRoleFlow
            if (!token.isNullOrBlank()) {
                _uiState.update { it.copy(isLoggedIn = true) }
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun quickFill(email: String, role: String) {
        _uiState.update {
            it.copy(
                email = email,
                password = "Password123!",
                errorMessage = null
            )
        }
    }

    fun login(onSuccess: (role: String) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.login(LoginRequest(email = state.email.trim(), password = state.password))
                if (response.success && response.token.isNotBlank()) {
                    val user = response.user

                    // Save session in SessionManager
                    sessionManager.saveSession(
                        token = response.token,
                        userId = user.id,
                        userRole = user.role,
                        managerId = user.managerId,
                        hierarchyPath = user.hierarchyPath
                    )

                    // Persist user entity in Room
                    database.userDao().insertUser(
                        UserEntity(
                            id = user.id,
                            email = user.email,
                            fullName = user.fullName,
                            phone = user.phone,
                            role = user.role,
                            managerId = user.managerId,
                            hierarchyPath = user.hierarchyPath,
                            vehicleType = user.vehicleType,
                            vehicleNumber = user.vehicleNumber,
                            fuelRatePerKm = user.fuelRatePerKm,
                            dailyAllowance = user.dailyAllowance,
                            isActive = user.isActive,
                            updatedAt = user.updatedAt ?: ""
                        )
                    )

                    // Trigger initial cloud sync in background
                    runCatching { syncManager.triggerImmediateDeltaSync() }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            loggedInRole = user.role
                        )
                    }

                    onSuccess(user.role)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Authentication failed. Please verify credentials."
                        )
                    }
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("401") == true -> "Invalid email or password."
                    e.message?.contains("Unable to resolve host") == true -> "Network error. Please check your internet connection."
                    else -> "Login error: ${e.localizedMessage ?: "Unexpected error"}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }
}
