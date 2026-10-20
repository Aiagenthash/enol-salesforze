package com.enol.salesforze.ui.gis

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * Domain representation of a Pakistan Administrative Level 2 District GIS feature.
 * Encapsulates geographic boundary polygons, assignment metadata, and MTD performance KPIs.
 */
data class DistrictGisFeature(
    val id: String,
    val name: String,
    val code: String,
    val province: String,
    val assignedRepName: String?,
    val assignedRepId: String?,
    val reportingAsmName: String?,
    val reportingAsmId: String?,
    val activeClientsCount: Int,
    val activeMechanicsCount: Int,
    val targetVolumeLiters: Double,
    val achievedVolumeLiters: Double,
    val polygons: List<List<LatLng>>
) {
    /**
     * MTD Achievement percentage against assigned target volume (0.0 to 100.0+).
     */
    val achievementPercentage: Double
        get() = if (targetVolumeLiters > 0.0) {
            (achievedVolumeLiters / targetVolumeLiters) * 100.0
        } else {
            0.0
        }

    /**
     * Whether a sales representative is actively assigned to this territory.
     */
    val isAssigned: Boolean
        get() = !assignedRepName.isNullOrBlank()

    /**
     * Geographic centroid computed from all polygon perimeter vertices.
     * Serves as marker anchor point on Google Maps.
     */
    val centroid: LatLng
        get() {
            val allPoints = polygons.flatten()
            if (allPoints.isEmpty()) return LatLng(30.3753, 69.3451)
            var sumLat = 0.0
            var sumLng = 0.0
            for (point in allPoints) {
                sumLat += point.latitude
                sumLng += point.longitude
            }
            return LatLng(sumLat / allPoints.size, sumLng / allPoints.size)
        }
}

/**
 * High-performance GeoJSON parser tailored for Pakistan ADM2 District FeatureCollections.
 * Parses geometry (Polygon and MultiPolygon) and business properties into [DistrictGisFeature].
 */
object GeoJsonParser {

    private const val DEFAULT_ASSET_FILE = "pakistan_districts_adm2.geojson"

    /**
     * Loads and parses the GeoJSON FeatureCollection directly from the Android assets directory.
     */
    fun parseFromAssets(
        context: Context,
        fileName: String = DEFAULT_ASSET_FILE
    ): List<DistrictGisFeature> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                parseInputStream(inputStream)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses an [InputStream] containing GeoJSON data.
     */
    fun parseInputStream(inputStream: InputStream): List<DistrictGisFeature> {
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return parseGeoJson(jsonString)
    }

    /**
     * Parses a raw GeoJSON String into a list of [DistrictGisFeature].
     */
    fun parseGeoJson(jsonString: String): List<DistrictGisFeature> {
        val root = JSONObject(jsonString)
        val features = root.optJSONArray("features") ?: return emptyList()
        val districtList = ArrayList<DistrictGisFeature>(features.length())

        for (i in 0 until features.length()) {
            val featObj = features.optJSONObject(i) ?: continue
            val id = featObj.optString("id", "")
            val props = featObj.optJSONObject("properties") ?: JSONObject()
            val geom = featObj.optJSONObject("geometry") ?: continue

            val name = props.optString("name", "")
            val code = props.optString("code", "")
            val province = props.optString("province", "")
            val assignedRepName = props.optString("assigned_rep_name").takeIf {
                it.isNotBlank() && it != "null"
            }
            val assignedRepId = props.optString("assigned_rep_id").takeIf {
                it.isNotBlank() && it != "null"
            }
            val reportingAsmName = props.optString("reporting_asm_name").takeIf {
                it.isNotBlank() && it != "null"
            }
            val reportingAsmId = props.optString("reporting_asm_id").takeIf {
                it.isNotBlank() && it != "null"
            }
            val activeClients = props.optInt("active_clients_count", 0)
            val activeMechanics = props.optInt("active_mechanics_count", 0)
            val targetVolume = props.optDouble("target_volume_liters", 0.0)
            val achievedVolume = props.optDouble("achieved_volume_liters", 0.0)

            val geomType = geom.optString("type", "")
            val coordsArray = geom.optJSONArray("coordinates") ?: continue
            val polygons = mutableListOf<List<LatLng>>()

            when (geomType) {
                "Polygon" -> {
                    for (r in 0 until coordsArray.length()) {
                        val ring = coordsArray.optJSONArray(r) ?: continue
                        val ringPts = parseRing(ring)
                        if (ringPts.isNotEmpty()) {
                            polygons.add(ringPts)
                        }
                    }
                }
                "MultiPolygon" -> {
                    for (p in 0 until coordsArray.length()) {
                        val polyArr = coordsArray.optJSONArray(p) ?: continue
                        for (r in 0 until polyArr.length()) {
                            val ring = polyArr.optJSONArray(r) ?: continue
                            val ringPts = parseRing(ring)
                            if (ringPts.isNotEmpty()) {
                                polygons.add(ringPts)
                            }
                        }
                    }
                }
            }

            districtList.add(
                DistrictGisFeature(
                    id = if (id.isNotBlank()) id else props.optString("id", code),
                    name = name,
                    code = code,
                    province = province,
                    assignedRepName = assignedRepName,
                    assignedRepId = assignedRepId,
                    reportingAsmName = reportingAsmName,
                    reportingAsmId = reportingAsmId,
                    activeClientsCount = activeClients,
                    activeMechanicsCount = activeMechanics,
                    targetVolumeLiters = targetVolume,
                    achievedVolumeLiters = achievedVolume,
                    polygons = polygons
                )
            )
        }

        return districtList
    }

    /**
     * Extracts a list of [LatLng] from a coordinate ring array `[[lng, lat], ...]`.
     * Note: Standard GeoJSON stores longitude first, then latitude.
     */
    private fun parseRing(ringArray: JSONArray): List<LatLng> {
        val points = ArrayList<LatLng>(ringArray.length())
        for (j in 0 until ringArray.length()) {
            val pt = ringArray.optJSONArray(j) ?: continue
            if (pt.length() >= 2) {
                val lng = pt.optDouble(0)
                val lat = pt.optDouble(1)
                points.add(LatLng(lat, lng))
            }
        }
        return points
    }
}
