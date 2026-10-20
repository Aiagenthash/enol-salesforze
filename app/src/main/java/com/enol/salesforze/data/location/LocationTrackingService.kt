package com.enol.salesforze.data.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.enol.salesforze.MainActivity
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.BreadcrumbEntity
import com.enol.salesforze.data.remote.session.SessionManager
import com.enol.salesforze.data.sync.SyncManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

/**
 * Foreground Location Tracking Service.
 * Continuously records sales representative GPS breadcrumbs every 5 minutes or 250 meters,
 * performs live anti-spoofing verification, and triggers background synchronization.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var syncManager: SyncManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "enol_location_tracking_channel"
        const val CHANNEL_NAME = "Field Location Tracking"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_TRACKING = "com.enol.salesforze.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.enol.salesforze.action.STOP_TRACKING"

        private const val UPDATE_INTERVAL_MS = 300_000L // 5 minutes
        private const val FASTEST_INTERVAL_MS = 120_000L // 2 minutes
        private const val MIN_DISPLACEMENT_METERS = 250f // 250 meters
        private const val UPLOAD_BATCH_THRESHOLD = 5

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _mockGpsAlert = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val mockGpsAlert: SharedFlow<String> = _mockGpsAlert.asSharedFlow()

        /**
         * Starts the continuous foreground location tracking service.
         */
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the foreground location tracking service.
         */
        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TRACKING -> {
                stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundTracking()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    private fun startForegroundTracking() {
        val notification = buildTrackingNotification(
            title = "ENOL Field Force Active",
            message = "Tracking field route & distance • Anti-spoofing active"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true
        startLocationUpdates()
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        _isServiceRunning.value = false
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            // Permissions not granted yet
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                processLocationUpdate(location)
            }
        }
    }

    private fun processLocationUpdate(location: Location) {
        serviceScope.launch {
            val isMock = AntiSpoofingDetector.isMockLocation(applicationContext, location)

            if (isMock) {
                // Log high-priority security alert and freeze session check-ins
                AntiSpoofingDetector.logSecurityAlert(
                    context = applicationContext,
                    location = location,
                    database = database,
                    sessionManager = sessionManager,
                    detectedReason = "MOCK_LOCATION_DETECTED_IN_SERVICE"
                )
                _mockGpsAlert.tryEmit("Mock GPS detected! Field actions temporarily blocked.")
                updateNotificationWarning()
            }

            val userId = sessionManager.getCachedUserId() ?: return@launch
            val activeShift = database.attendanceDao().getActiveAttendanceForUserOnce(userId)
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            val breadcrumb = BreadcrumbEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                attendanceId = activeShift?.id,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy.toDouble(),
                speed = if (location.hasSpeed()) location.speed.toDouble() else null,
                bearing = if (location.hasBearing()) location.bearing.toDouble() else null,
                batteryLevel = getBatteryLevel(),
                isMockGps = isMock,
                recordedAt = nowIso,
                isSynced = false
            )

            database.breadcrumbDao().insertBreadcrumb(breadcrumb)

            // Trigger background upload when batch reaches threshold
            val unsynced = database.breadcrumbDao().getUnsyncedBreadcrumbsOnce(UPLOAD_BATCH_THRESHOLD)
            if (unsynced.size >= UPLOAD_BATCH_THRESHOLD) {
                syncManager.triggerImmediateUploadSync()
            }
        }
    }

    private fun getBatteryLevel(): Double? {
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            level?.toDouble()
        } catch (_: Exception) {
            null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous GPS breadcrumb tracking and anti-spoofing verification for active field shifts."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildTrackingNotification(title: String, message: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Tracking", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotificationWarning() {
        val warningNotification = buildTrackingNotification(
            title = "⚠️ Fake GPS Detected!",
            message = "Simulated location active. Field visits and orders are blocked."
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, warningNotification)
    }
}
