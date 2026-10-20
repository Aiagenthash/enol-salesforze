package com.enol.salesforze.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-precision Geodesic & Geofencing utility using the Haversine formula.
 * Enforces field radius constraints for retailer visit check-ins and audit verification.
 */
object GeofenceUtil {

    private const val EARTH_RADIUS_METERS = 6371000.0 // Earth radius in meters
    const val DEFAULT_VISIT_GEOFENCE_RADIUS_METERS = 100.0 // Strict 100m check-in radius

    /**
     * Calculates the great-circle distance between two geographic coordinates in meters.
     */
    fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks whether the user's current GPS location falls within the allowed radius of a client pin.
     */
    fun isWithinGeofence(
        userLat: Double,
        userLng: Double,
        clientLat: Double,
        clientLng: Double,
        maxRadiusMeters: Double = DEFAULT_VISIT_GEOFENCE_RADIUS_METERS
    ): Boolean {
        val distance = calculateHaversineDistance(userLat, userLng, clientLat, clientLng)
        return distance <= maxRadiusMeters
    }

    /**
     * Human-readable formatted distance string (e.g. "45 m away", "1.2 km away").
     */
    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000.0) {
            "${distanceMeters.toInt()} m"
        } else {
            "%.1f km".format(distanceMeters / 1000.0)
        }
    }
}
