package com.enol.salesforze.ui.gis

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enol.salesforze.data.local.AppDatabase
import com.enol.salesforze.data.local.entities.DistrictEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

/**
 * Rendering modes for the interactive Pakistan ADM2 District Map.
 */
enum class MapRenderingMode {
    /**
     * Highlights field sales force assignments with distinct harmonic rep colors.
     */
    TERRITORY_ASSIGNMENT,

    /**
     * Color-codes districts by MTD Target KPI Achievement volume:
     * Green (>=80%), Amber (50-79%), Red (<50%), Slate Grey (Unassigned / 0 L).
     */
    KPI_HEATMAP
}

/**
 * UI State container for the Pakistan District GIS visualization.
 */
data class DistrictMapUiState(
    val districts: List<DistrictGisFeature> = emptyList(),
    val filteredDistricts: List<DistrictGisFeature> = emptyList(),
    val renderingMode: MapRenderingMode = MapRenderingMode.KPI_HEATMAP,
    val selectedDistrict: DistrictGisFeature? = null,
    val selectedProvince: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val totalActiveClients: Int = 0,
    val totalActiveMechanics: Int = 0,
    val totalTargetLiters: Double = 0.0,
    val totalAchievedLiters: Double = 0.0
) {
    val overallAchievementPercentage: Double
        get() = if (totalTargetLiters > 0.0) {
            (totalAchievedLiters / totalTargetLiters) * 100.0
        } else 0.0
}

@HiltViewModel
class DistrictMapViewModel @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistrictMapUiState(isLoading = true))
    val uiState: StateFlow<DistrictMapUiState> = _uiState.asStateFlow()

    companion object {
        // KPI Heatmap Standard Color Tokens
        val ColorGreen = Color(0xFF2ECC71)      // Achievement >= 80%
        val ColorAmber = Color(0xFFF39C12)      // Achievement 50% - 79%
        val ColorRed = Color(0xFFE74C3C)        // Achievement < 50%
        val ColorSlateGrey = Color(0xFF94A3B8)  // Unassigned / 0 Target

        // Distinct harmonic color palette for territory assignments
        val HarmonicTerritoryPalette = listOf(
            Color(0xFF1E88E5), // Blue 600
            Color(0xFF00897B), // Teal 600
            Color(0xFF8E24AA), // Purple 600
            Color(0xFFD81B60), // Magenta 600
            Color(0xFFF4511E), // Deep Orange 600
            Color(0xFF3949AB), // Indigo 600
            Color(0xFF00ACC1), // Cyan 600
            Color(0xFF43A047), // Green 600
            Color(0xFFFB8C00), // Industrial Amber 600
            Color(0xFF5E35B1), // Deep Violet 600
            Color(0xFF039BE5), // Sky Blue 600
            Color(0xFF7CB342), // Olive Lime 600
            Color(0xFF00838F), // Marine Cyan 600
            Color(0xFF6D4C41), // Industrial Bronze 600
            Color(0xFF546E7A), // Blue Grey 600
            Color(0xFFE91E63), // Pink 600
            Color(0xFF26A69A), // Mint Teal 500
            Color(0xFF673AB7), // Purple 500
            Color(0xFFFF9800), // Warm Orange 500
            Color(0xFF5C6BC0)  // Soft Indigo 500
        )
    }

    init {
        loadDistricts()
    }

    /**
     * Loads Pakistan ADM2 districts from the bundled GeoJSON asset,
     * synchronizes with Room database entities, and calculates aggregate metrics.
     */
    fun loadDistricts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val parsedDistricts = withContext(Dispatchers.IO) {
                val features = GeoJsonParser.parseFromAssets(context)
                seedDatabaseDistrictsIfEmpty(features)
                features
            }

            val totalClients = parsedDistricts.sumOf { it.activeClientsCount }
            val totalMechanics = parsedDistricts.sumOf { it.activeMechanicsCount }
            val totalTarget = parsedDistricts.sumOf { it.targetVolumeLiters }
            val totalAchieved = parsedDistricts.sumOf { it.achievedVolumeLiters }

            _uiState.update { state ->
                val filtered = applyFilter(parsedDistricts, state.selectedProvince, state.searchQuery)
                state.copy(
                    districts = parsedDistricts,
                    filteredDistricts = filtered,
                    totalActiveClients = totalClients,
                    totalActiveMechanics = totalMechanics,
                    totalTargetLiters = totalTarget,
                    totalAchievedLiters = totalAchieved,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Safely seeds the SQLite `districts` table with GeoJSON features if currently empty.
     */
    private suspend fun seedDatabaseDistrictsIfEmpty(features: List<DistrictGisFeature>) {
        try {
            val existing = database.districtDao().getAllDistricts().firstOrNull()
            if (existing.isNullOrEmpty() && features.isNotEmpty()) {
                val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val entities = features.map { feature ->
                    DistrictEntity(
                        id = feature.id,
                        name = feature.name,
                        code = feature.code,
                        province = feature.province,
                        polygonGeoJson = null,
                        assignedZsmId = null,
                        assignedAsmId = feature.reportingAsmId,
                        isActive = true,
                        updatedAt = nowIso
                    )
                }
                database.districtDao().insertDistricts(entities)
            }
        } catch (_: Exception) {
            // Non-blocking fallback to memory
        }
    }

    /**
     * Toggles between [MapRenderingMode.TERRITORY_ASSIGNMENT] and [MapRenderingMode.KPI_HEATMAP].
     */
    fun setRenderingMode(mode: MapRenderingMode) {
        _uiState.update { it.copy(renderingMode = mode) }
    }

    /**
     * Selects a district to inspect in the detail bottom sheet, or null to dismiss.
     */
    fun selectDistrict(district: DistrictGisFeature?) {
        _uiState.update { it.copy(selectedDistrict = district) }
    }

    /**
     * Filters visible districts by province ("Punjab", "Sindh", etc.), or null for all provinces.
     */
    fun setProvinceFilter(province: String?) {
        _uiState.update { state ->
            val newFilter = if (state.selectedProvince == province) null else province
            val filtered = applyFilter(state.districts, newFilter, state.searchQuery)
            state.copy(selectedProvince = newFilter, filteredDistricts = filtered)
        }
    }

    /**
     * Filters districts by search query against name, code, or assigned rep.
     */
    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.districts, state.selectedProvince, query)
            state.copy(searchQuery = query, filteredDistricts = filtered)
        }
    }

    /**
     * Computes the fill and stroke colors for a district polygon based on the active [MapRenderingMode].
     *
     * In [MapRenderingMode.TERRITORY_ASSIGNMENT]:
     * - Distinct harmonic colors for each assigned sales rep
     * - Slate Grey (#94A3B8) for unassigned territories
     *
     * In [MapRenderingMode.KPI_HEATMAP]:
     * - Green (#2ECC71): Achievement >= 80%
     * - Amber (#F39C12): Achievement 50% - 79%
     * - Red (#E74C3C): Achievement < 50%
     * - Slate Grey (#94A3B8): Unassigned or 0 target
     *
     * Returns `Pair(fillColor, strokeColor)` where fill has alpha = 0.35f and stroke has alpha = 0.9f.
     */
    fun getDistrictColors(
        district: DistrictGisFeature,
        mode: MapRenderingMode = uiState.value.renderingMode
    ): Pair<Color, Color> {
        val baseColor = when (mode) {
            MapRenderingMode.TERRITORY_ASSIGNMENT -> {
                if (!district.isAssigned || district.assignedRepId.isNullOrBlank()) {
                    ColorSlateGrey
                } else {
                    val hash = abs(district.assignedRepId.hashCode()) % HarmonicTerritoryPalette.size
                    HarmonicTerritoryPalette[hash]
                }
            }
            MapRenderingMode.KPI_HEATMAP -> {
                if (!district.isAssigned || district.targetVolumeLiters <= 0.0) {
                    ColorSlateGrey
                } else {
                    val ach = district.achievementPercentage
                    when {
                        ach >= 80.0 -> ColorGreen
                        ach >= 50.0 -> ColorAmber
                        else -> ColorRed
                    }
                }
            }
        }

        val fillColor = baseColor.copy(alpha = 0.35f)
        val strokeColor = baseColor.copy(alpha = 0.9f)
        return Pair(fillColor, strokeColor)
    }

    /**
     * Forces a fresh reload from assets/database.
     */
    fun refresh() {
        loadDistricts()
    }

    private fun applyFilter(
        list: List<DistrictGisFeature>,
        province: String?,
        query: String
    ): List<DistrictGisFeature> {
        return list.filter { district ->
            val matchesProvince = province.isNullOrBlank() || district.province.equals(province, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    district.name.contains(query, ignoreCase = true) ||
                    district.code.contains(query, ignoreCase = true) ||
                    (district.assignedRepName?.contains(query, ignoreCase = true) == true) ||
                    (district.reportingAsmName?.contains(query, ignoreCase = true) == true)
            matchesProvince && matchesQuery
        }
    }
}
