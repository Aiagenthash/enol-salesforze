package com.enol.salesforze.ui.attendance

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.enol.salesforze.data.location.AntiSpoofingDetector
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-precision GPS lock acquisition and anti-spoofing verification engine.
 * Ensures field shift start/end events are tied to authentic, high-accuracy geographic coordinates.
 */
@Singleton
class GpsLockValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    companion object {
        const val DEFAULT_MAX_ACCURACY_METERS = 25.0f
        const val GPS_TIMEOUT_MS = 15_000L
    }

    /**
     * Checks if the device's GPS hardware provider is currently enabled in system settings.
     */
    fun isGpsProviderEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
    }

    /**
     * Acquires an immediate, fresh GPS fix with accuracy validation and mock location screening.
     *
     * @param maxAccuracyMeters Maximum allowed horizontal accuracy in meters (default 25.0m).
     * @return [Result.success] with authentic [Location], or [Result.failure] with diagnostic error.
     */
    @SuppressLint("MissingPermission")
    suspend fun acquireValidGpsLock(
        maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS
    ): Result<Location> {
        if (!isGpsProviderEnabled()) {
            return Result.failure(
                IllegalStateException("GPS is turned off. Please enable high-accuracy device location.")
            )
        }

        val cancellationTokenSource = CancellationTokenSource()

        val locationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(10_000L) // Must be fresh within 10s
            .build()

        val location: Location? = try {
            withTimeoutOrNull(GPS_TIMEOUT_MS) {
                fusedLocationClient.getCurrentLocation(locationRequest, cancellationTokenSource.token).await()
            }
        } catch (e: SecurityException) {
            return Result.failure(SecurityException("Location permissions are required to acquire GPS lock."))
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            cancellationTokenSource.cancel()
        }

        if (location == null) {
            return Result.failure(
                IllegalStateException("GPS lock timed out. Please step outdoors for a clearer satellite view.")
            )
        }

        // Anti-Spoofing Verification
        if (AntiSpoofingDetector.isMockLocation(context, location)) {
            return Result.failure(
                SecurityException("⚠️ Mock GPS Detected! Simulated location providers are strictly prohibited.")
            )
        }

        // Accuracy Threshold Check
        if (location.hasAccuracy() && location.accuracy > maxAccuracyMeters) {
            return Result.failure(
                IllegalStateException(
                    "GPS accuracy is ${location.accuracy.toInt()}m (required <= ${maxAccuracyMeters.toInt()}m). " +
                    "Please wait a moment outdoors for stronger satellite triangulation."
                )
            )
        }

        return Result.success(location)
    }
}
