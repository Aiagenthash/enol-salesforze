package com.enol.salesforze.data.remote.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "enol_salesforze_session")

/**
 * Manages user credentials, authentication tokens, API keys, and session state.
 * Uses reactive Preferences DataStore for persistence and thread-safe in-memory
 * caching for zero-latency interceptor access.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_MANAGER_ID = stringPreferencesKey("manager_id")
        private val KEY_HIERARCHY_PATH = stringPreferencesKey("hierarchy_path")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_LAST_SYNC_TIMESTAMP = stringPreferencesKey("last_sync_timestamp")
        private val KEY_MOCK_LOCATION_BLOCKED = booleanPreferencesKey("is_mock_location_blocked")
    }

    // In-memory atomic state cache for instant interceptor reads without blocking I/O
    private val inMemoryToken = MutableStateFlow<String?>(null)
    private val inMemoryApiKey = MutableStateFlow<String?>(null)
    private val inMemoryUserId = MutableStateFlow<String?>(null)
    private val inMemoryUserRole = MutableStateFlow<String?>(null)
    private val inMemoryManagerId = MutableStateFlow<String?>(null)
    private val inMemoryHierarchyPath = MutableStateFlow<String?>(null)
    private val inMemoryMockLocationBlocked = MutableStateFlow(false)

    // Event bus to notify UI/repositories when session expires (401)
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            context.sessionDataStore.data
                .catch { exception ->
                    if (exception is IOException) emit(emptyPreferences()) else throw exception
                }
                .collect { prefs ->
                    inMemoryToken.value = prefs[KEY_AUTH_TOKEN]
                    inMemoryApiKey.value = prefs[KEY_API_KEY]
                    inMemoryUserId.value = prefs[KEY_USER_ID]
                    inMemoryUserRole.value = prefs[KEY_USER_ROLE]
                    inMemoryManagerId.value = prefs[KEY_MANAGER_ID]
                    inMemoryHierarchyPath.value = prefs[KEY_HIERARCHY_PATH]
                    inMemoryMockLocationBlocked.value = prefs[KEY_MOCK_LOCATION_BLOCKED] ?: false
                }
        }
    }

    // Reactive Flows
    val tokenFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_AUTH_TOKEN] }

    val isAuthenticatedFlow: Flow<Boolean> = tokenFlow.map { !it.isNullOrBlank() }

    val userIdFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_USER_ID] }

    val userRoleFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_USER_ROLE] }

    val managerIdFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_MANAGER_ID] }

    val hierarchyPathFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_HIERARCHY_PATH] }

    val apiKeyFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_API_KEY] }

    val lastSyncTimestampFlow: Flow<String?> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_LAST_SYNC_TIMESTAMP] }

    val isMockLocationBlockedFlow: Flow<Boolean> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[KEY_MOCK_LOCATION_BLOCKED] ?: false }

    /**
     * Synchronously read in-memory cached user ID.
     */
    fun getCachedUserId(): String? = inMemoryUserId.value

    /**
     * Retrieve user ID from memory or fallback to DataStore.
     */
    suspend fun getUserId(): String? {
        inMemoryUserId.value?.let { return it }
        val userId = context.sessionDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { it[KEY_USER_ID] }
            .firstOrNull()
        inMemoryUserId.value = userId
        return userId
    }

    /**
     * Check if mock location is currently flagged and blocking operations.
     */
    fun isMockLocationBlocked(): Boolean = inMemoryMockLocationBlocked.value

    /**
     * Set or clear the mock location security block state.
     */
    suspend fun setMockLocationBlocked(blocked: Boolean) {
        inMemoryMockLocationBlocked.value = blocked
        context.sessionDataStore.edit { preferences ->
            preferences[KEY_MOCK_LOCATION_BLOCKED] = blocked
        }
    }

    /**
     * Asynchronously flag mock location blocked.
     */
    fun flagMockLocationBlocked(blocked: Boolean = true) {
        inMemoryMockLocationBlocked.value = blocked
        CoroutineScope(Dispatchers.IO).launch {
            setMockLocationBlocked(blocked)
        }
    }

    /**
     * Persist last synchronization timestamp.
     */
    suspend fun saveLastSyncTimestamp(timestamp: String) {
        context.sessionDataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    /**
     * Retrieve last sync timestamp.
     */
    suspend fun getLastSyncTimestamp(): String? {
        return context.sessionDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { it[KEY_LAST_SYNC_TIMESTAMP] }
            .firstOrNull()
    }

    /**
     * Persist JWT authentication token.
     */
    suspend fun saveAuthToken(token: String) {
        inMemoryToken.value = token
        context.sessionDataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
        }
    }

    /**
     * Retrieve auth token from memory or fallback to DataStore.
     */
    suspend fun getAuthToken(): String? {
        inMemoryToken.value?.let { return it }
        val token = context.sessionDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { it[KEY_AUTH_TOKEN] }
            .firstOrNull()
        inMemoryToken.value = token
        return token
    }

    /**
     * Synchronously read in-memory cached token.
     */
    fun getCachedAuthToken(): String? = inMemoryToken.value

    /**
     * Persist X-API-Key.
     */
    suspend fun saveApiKey(apiKey: String) {
        inMemoryApiKey.value = apiKey
        context.sessionDataStore.edit { preferences ->
            preferences[KEY_API_KEY] = apiKey
        }
    }

    /**
     * Retrieve API key from memory or fallback to DataStore.
     */
    suspend fun getApiKey(): String? {
        inMemoryApiKey.value?.let { return it }
        val key = context.sessionDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { it[KEY_API_KEY] }
            .firstOrNull()
        inMemoryApiKey.value = key
        return key
    }

    /**
     * Synchronously read in-memory cached API key.
     */
    fun getCachedApiKey(): String? = inMemoryApiKey.value

    /**
     * Persist full authenticated user session.
     */
    suspend fun saveSession(
        token: String,
        userId: String,
        userRole: String,
        managerId: String?,
        hierarchyPath: String
    ) {
        inMemoryToken.value = token
        inMemoryUserId.value = userId
        inMemoryUserRole.value = userRole
        inMemoryManagerId.value = managerId
        inMemoryHierarchyPath.value = hierarchyPath

        context.sessionDataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_ROLE] = userRole
            if (managerId != null) {
                preferences[KEY_MANAGER_ID] = managerId
            } else {
                preferences.remove(KEY_MANAGER_ID)
            }
            preferences[KEY_HIERARCHY_PATH] = hierarchyPath
        }
    }

    /**
     * Notify observers that session expired (HTTP 401).
     */
    fun notifySessionExpired() {
        _sessionExpiredEvent.tryEmit(Unit)
    }

    /**
     * Clear active session upon logout or 401 unauthorized.
     */
    suspend fun clearSession() {
        inMemoryToken.value = null
        inMemoryUserId.value = null
        inMemoryUserRole.value = null
        inMemoryManagerId.value = null
        inMemoryHierarchyPath.value = null
        inMemoryMockLocationBlocked.value = false

        context.sessionDataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_USER_ID)
            preferences.remove(KEY_USER_ROLE)
            preferences.remove(KEY_MANAGER_ID)
            preferences.remove(KEY_HIERARCHY_PATH)
            preferences.remove(KEY_MOCK_LOCATION_BLOCKED)
        }
    }
}
