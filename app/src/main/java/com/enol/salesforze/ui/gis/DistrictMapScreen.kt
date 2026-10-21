package com.enol.salesforze.ui.gis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.EnolNavyLight
import com.enol.salesforze.ui.theme.ErrorCrimson
import com.enol.salesforze.ui.theme.LubricantAmber
import com.enol.salesforze.ui.theme.SlateSteel
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.SuccessEmerald
import com.enol.salesforze.ui.theme.WarningAmber
import com.enol.salesforze.ui.theme.elevation_2
import com.enol.salesforze.ui.theme.elevation_4
import com.enol.salesforze.ui.theme.grid_0_5
import com.enol.salesforze.ui.theme.grid_1
import com.enol.salesforze.ui.theme.grid_1_5
import com.enol.salesforze.ui.theme.grid_2
import com.enol.salesforze.ui.theme.grid_3
import com.enol.salesforze.ui.theme.grid_4
import com.enol.salesforze.ui.theme.grid_5
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.util.Locale

// Geographic Center of Pakistan for camera initialization
private val PAKISTAN_CENTER = LatLng(30.3753, 69.3451)
private const val PAKISTAN_DEFAULT_ZOOM = 6.0f

// Regional coordinates for quick province pan/zoom
private val PROVINCE_CENTERS = mapOf(
    "Punjab" to Pair(LatLng(31.1704, 72.7097), 7.0f),
    "Sindh" to Pair(LatLng(25.8943, 68.5247), 7.0f),
    "Khyber Pakhtunkhwa" to Pair(LatLng(34.4000, 72.0000), 7.5f),
    "Balochistan" to Pair(LatLng(28.4907, 65.0958), 6.5f),
    "Islamabad Capital Territory" to Pair(LatLng(33.6844, 73.0479), 11.0f),
    "Gilgit-Baltistan" to Pair(LatLng(35.8000, 75.0000), 7.5f),
    "Azad Jammu & Kashmir" to Pair(LatLng(33.8000, 73.6500), 8.0f)
)

/**
 * Interactive Pakistan District GIS Screen powered by Google Maps Compose.
 * Supports Territory Assignment visualization and Target KPI Volume Achievement heatmapping.
 */
@Composable
fun DistrictMapScreen(
    modifier: Modifier = Modifier,
    viewModel: DistrictMapViewModel = hiltViewModel(),
    onViewBeatSchedule: (DistrictGisFeature) -> Unit = {},
    onFilterClients: (DistrictGisFeature) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    DistrictMapContent(
        uiState = uiState,
        onRenderingModeChanged = viewModel::setRenderingMode,
        onDistrictSelected = viewModel::selectDistrict,
        onProvinceFilterSelected = viewModel::setProvinceFilter,
        onSearchQueryChanged = viewModel::setSearchQuery,
        getDistrictColors = { district -> viewModel.getDistrictColors(district, uiState.renderingMode) },
        onViewBeatSchedule = onViewBeatSchedule,
        onFilterClients = onFilterClients,
        onRefresh = viewModel::refresh,
        modifier = modifier,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictMapContent(
    uiState: DistrictMapUiState,
    onRenderingModeChanged: (MapRenderingMode) -> Unit,
    onDistrictSelected: (DistrictGisFeature?) -> Unit,
    onProvinceFilterSelected: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    getDistrictColors: (DistrictGisFeature) -> Pair<Color, Color>,
    onViewBeatSchedule: (DistrictGisFeature) -> Unit,
    onFilterClients: (DistrictGisFeature) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(PAKISTAN_CENTER, PAKISTAN_DEFAULT_ZOOM)
    }
    val coroutineScope = rememberCoroutineScope()
    var isLegendExpanded by remember { mutableStateOf(true) }

    // Animate camera when selected district changes
    LaunchedEffect(uiState.selectedDistrict) {
        uiState.selectedDistrict?.let { district ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(district.centroid, 8.5f),
                durationMs = 600
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pakistan District GIS & Territory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.renderingMode == MapRenderingMode.KPI_HEATMAP) {
                                "MTD Volume KPI Heatmap • ${uiState.districts.size} Districts"
                            } else {
                                "Sales Force Territory Alignment • ${uiState.districts.size} Districts"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Districts"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Google Maps Surface
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    mapToolbarEnabled = false,
                    compassEnabled = true,
                    tiltGesturesEnabled = false
                )
            ) {
                // Render District Polygons
                for (district in uiState.filteredDistricts) {
                    val isSelected = uiState.selectedDistrict?.id == district.id
                    val (fillColor, strokeColor) = getDistrictColors(district)

                    for (polygonPoints in district.polygons) {
                        Polygon(
                            points = polygonPoints,
                            fillColor = if (isSelected) fillColor.copy(alpha = 0.55f) else fillColor,
                            strokeColor = if (isSelected) LubricantAmber else strokeColor,
                            strokeWidth = if (isSelected) 8f else 4f,
                            clickable = true,
                            onClick = {
                                onDistrictSelected(district)
                            }
                        )
                    }

                    // Centroid Marker for district identification
                    Marker(
                        state = rememberMarkerState(key = district.id, position = district.centroid),
                        title = district.name,
                        snippet = "${district.code} • ${district.province}",
                        onClick = {
                            onDistrictSelected(district)
                            true
                        }
                    )
                }
            }

            // Floating Top Controls Layer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = grid_3, vertical = grid_2)
                    .align(Alignment.TopCenter)
            ) {
                // Segmented Button Row: Territory View vs KPI Heatmap View
                Surface(
                    shape = RoundedCornerShape(grid_3),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = elevation_4,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_1)
                    ) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { onRenderingModeChanged(MapRenderingMode.TERRITORY_ASSIGNMENT) },
                            selected = uiState.renderingMode == MapRenderingMode.TERRITORY_ASSIGNMENT,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Territory View",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )

                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { onRenderingModeChanged(MapRenderingMode.KPI_HEATMAP) },
                            selected = uiState.renderingMode == MapRenderingMode.KPI_HEATMAP,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "KPI Heatmap View",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(grid_1))

                // Province Filter Chips
                val provinces = listOf(
                    "Punjab",
                    "Sindh",
                    "Khyber Pakhtunkhwa",
                    "Balochistan",
                    "Islamabad Capital Territory",
                    "Gilgit-Baltistan",
                    "Azad Jammu & Kashmir"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(grid_1)
                ) {
                    FilterChip(
                        selected = uiState.selectedProvince == null,
                        onClick = {
                            onProvinceFilterSelected(null)
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(PAKISTAN_CENTER, PAKISTAN_DEFAULT_ZOOM),
                                    700
                                )
                            }
                        },
                        label = { Text("All Pakistan (${uiState.districts.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EnolNavy,
                            selectedLabelColor = Color.White
                        )
                    )

                    for (province in provinces) {
                        val count = uiState.districts.count { it.province == province }
                        val isSelected = uiState.selectedProvince == province

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onProvinceFilterSelected(province)
                                PROVINCE_CENTERS[province]?.let { (center, zoom) ->
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(center, zoom),
                                            700
                                        )
                                    }
                                }
                            },
                            label = {
                                val shortName = when (province) {
                                    "Khyber Pakhtunkhwa" -> "KPK"
                                    "Islamabad Capital Territory" -> "Islamabad (ICT)"
                                    "Gilgit-Baltistan" -> "GB"
                                    "Azad Jammu & Kashmir" -> "AJK"
                                    else -> province
                                }
                                Text("$shortName ($count)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EnolNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(grid_1))

                // Collapsible Interactive Legend Card
                Card(
                    shape = RoundedCornerShape(grid_2),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = grid_3, vertical = grid_1_5)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isLegendExpanded = !isLegendExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.renderingMode == MapRenderingMode.KPI_HEATMAP) {
                                        Icons.Default.Speed
                                    } else {
                                        Icons.Default.Group
                                    },
                                    contentDescription = null,
                                    tint = EnolNavyLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(grid_1))
                                Text(
                                    text = if (uiState.renderingMode == MapRenderingMode.KPI_HEATMAP) {
                                        "Volume Achievement Legend"
                                    } else {
                                        "Territory Distribution Legend"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Icon(
                                imageVector = if (isLegendExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Legend",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isLegendExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = grid_1)) {
                                if (uiState.renderingMode == MapRenderingMode.KPI_HEATMAP) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        LegendPill(
                                            color = SuccessEmerald,
                                            label = "≥ 80% On Track",
                                            count = uiState.districts.count { it.isAssigned && it.achievementPercentage >= 80.0 }
                                        )
                                        LegendPill(
                                            color = WarningAmber,
                                            label = "50-79% At Risk",
                                            count = uiState.districts.count { it.isAssigned && it.achievementPercentage in 50.0..79.99 }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(grid_0_5))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        LegendPill(
                                            color = ErrorCrimson,
                                            label = "< 50% Critical",
                                            count = uiState.districts.count { it.isAssigned && it.achievementPercentage < 50.0 && it.targetVolumeLiters > 0.0 }
                                        )
                                        LegendPill(
                                            color = SlateSteelLight,
                                            label = "Unassigned / 0 L",
                                            count = uiState.districts.count { !it.isAssigned || it.targetVolumeLiters <= 0.0 }
                                        )
                                    }
                                } else {
                                    val assignedCount = uiState.districts.count { it.isAssigned }
                                    val unassignedCount = uiState.districts.count { !it.isAssigned }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(EnolNavyLight)
                                            )
                                            Spacer(modifier = Modifier.width(grid_1))
                                            Text(
                                                text = "Harmonic Color per Assigned Rep ($assignedCount)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(SlateSteelLight)
                                            )
                                            Spacer(modifier = Modifier.width(grid_1))
                                            Text(
                                                text = "Unassigned ($unassignedCount)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Loading indicator overlay
            if (uiState.isLoading) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = LubricantAmber)
                            Spacer(modifier = Modifier.height(grid_2))
                            Text(
                                text = "Loading Pakistan ADM2 GIS Boundaries...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Material 3 Modal Bottom Sheet for District Detail Inspection
            uiState.selectedDistrict?.let { district ->
                DistrictDetailBottomSheet(
                    district = district,
                    onDismiss = { onDistrictSelected(null) },
                    onViewBeatSchedule = { onViewBeatSchedule(district) },
                    onFilterClients = { onFilterClients(district) }
                )
            }
        }
    }
}

/**
 * Small indicator pill used inside the interactive map legend.
 */
@Composable
private fun LegendPill(
    color: Color,
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(grid_1))
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Material 3 Modal Bottom Sheet displaying district metadata, sales force hierarchy,
 * 4-stat performance grid, and action triggers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictDetailBottomSheet(
    district: DistrictGisFeature,
    onDismiss: () -> Unit,
    onViewBeatSchedule: () -> Unit,
    onFilterClients: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = grid_2)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = grid_4)
                .padding(bottom = grid_5)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: District Name, Code Badge, Province Badge, Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(grid_1)
                    ) {
                        Text(
                            text = district.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // District Code Badge
                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = EnolNavy
                        ) {
                            Text(
                                text = district.code,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = grid_1_5, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${district.province} • Administrative Level 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sheet",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(grid_3))

            // Assigned Hierarchy Card (Sales Rep & Reporting ASM)
            Card(
                shape = RoundedCornerShape(grid_2),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(grid_3)) {
                    // Sales Rep Assignment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (district.isAssigned) EnolNavyLight else SlateSteelLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(grid_2))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Assigned Sales Representative",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = district.assignedRepName ?: "Unassigned Territory",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (district.isAssigned) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            if (district.assignedRepId != null) {
                                Text(
                                    text = "ID: ${district.assignedRepId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = grid_2),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Reporting Area Sales Manager (ASM)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EnolNavy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SupervisorAccount,
                                    contentDescription = null,
                                    tint = LubricantAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(grid_2))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reporting Area Sales Manager (ASM)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = district.reportingAsmName ?: "Unassigned Regional Management",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_3))

            // 4-Stat Metric Grid: Active Clients, Reg Mechanics, Volume Booked, Achievement %
            Text(
                text = "Territory Operational Metrics (MTD)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(grid_1_5))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_2)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Clients",
                    value = "${district.activeClientsCount}",
                    icon = Icons.Default.Storefront,
                    iconTint = EnolNavyLight,
                    subtitle = "Authorized retailers"
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Reg. Mechanics",
                    value = "${district.activeMechanicsCount}",
                    icon = Icons.Default.Build,
                    iconTint = LubricantAmber,
                    subtitle = "Mechanic network"
                )
            }

            Spacer(modifier = Modifier.height(grid_2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_2)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "MTD Volume Booked",
                    value = String.format(Locale.US, "%,.0f L", district.achievedVolumeLiters),
                    icon = Icons.Default.LocalGasStation,
                    iconTint = EnolNavy,
                    subtitle = "Target: " + String.format(Locale.US, "%,.0f L", district.targetVolumeLiters)
                )

                val achRate = district.achievementPercentage
                val (achColor, achStatus, achIcon) = when {
                    !district.isAssigned || district.targetVolumeLiters <= 0.0 ->
                        Triple(SlateSteelLight, "Unassigned", Icons.Default.Help)
                    achRate >= 80.0 ->
                        Triple(SuccessEmerald, "On Track", Icons.Default.CheckCircle)
                    achRate >= 50.0 ->
                        Triple(WarningAmber, "At Risk", Icons.Default.Warning)
                    else ->
                        Triple(ErrorCrimson, "Critical", Icons.Default.Error)
                }

                Card(
                    shape = RoundedCornerShape(grid_2),
                    colors = CardDefaults.cardColors(
                        containerColor = achColor.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, achColor.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_2)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KPI Achievement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = achIcon,
                                contentDescription = null,
                                tint = achColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(grid_1))

                        Text(
                            text = if (district.targetVolumeLiters > 0.0) {
                                String.format(Locale.US, "%.1f%%", achRate)
                            } else {
                                "N/A"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = achColor
                        )

                        Spacer(modifier = Modifier.height(grid_1))

                        val progressRatio = if (district.targetVolumeLiters > 0.0) {
                            (achRate / 100.0).coerceIn(0.0, 1.0).toFloat()
                        } else 0f

                        LinearProgressIndicator(
                            progress = progressRatio,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = achColor,
                            trackColor = achColor.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(grid_0_5))

                        Text(
                            text = achStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = achColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // Action Buttons: "View Beat Schedule" and "Filter Clients in District"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_2)
            ) {
                Button(
                    onClick = onViewBeatSchedule,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(grid_2),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EnolNavy,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(grid_1))
                    Text(
                        text = "Beat Schedule",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onFilterClients,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(grid_2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EnolNavy)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = EnolNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(grid_1))
                    Text(
                        text = "Filter Clients",
                        fontWeight = FontWeight.SemiBold,
                        color = EnolNavy,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Reusable Metric Tile for the District Detail Sheet.
 */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(grid_2),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(grid_2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(grid_1))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(grid_0_5))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
