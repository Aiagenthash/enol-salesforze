package com.enol.salesforze.data.location

import android.content.Context
import com.enol.salesforze.data.remote.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controller to orchestrate background location tracking and anti-spoofing surveillance
 * from ViewModels and Jetpack Compose screens.
 */
@Singleton
class LocationServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    /**
     * Observable state indicating whether the foreground tracking service is currently active.
     */
    val isServiceRunning: StateFlow<Boolean> = LocationTrackingService.isServiceRunning

    /**
     * Hot event stream emitting security alerts whenever fake GPS / mock provider is detected.
     */
    val mockGpsDetectedAlert: SharedFlow<String> = LocationTrackingService.mockGpsAlert

    /**
     * Flow indicating whether user operations (visits, orders) are currently blocked due to mock location.
     */
    val isMockLocationBlockedFlow: Flow<Boolean> = sessionManager.isMockLocationBlockedFlow

    /**
     * Synchronously checks whether mock GPS currently blocks operations.
     */
    fun isMockLocationBlocked(): Boolean = sessionManager.isMockLocationBlocked()

    /**
     * Starts the continuous foreground location tracking service.
     */
    fun startTracking() {
        LocationTrackingService.startService(context)
    }

    /**
     * Stops the continuous foreground location tracking service.
     */
    fun stopTracking() {
        LocationTrackingService.stopService(context)
    }
}
