package com.enol.salesforze.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enol.salesforze.data.local.entities.DistrictEntity
import com.enol.salesforze.ui.dashboard.DashboardUiState
import com.enol.salesforze.ui.dashboard.UserRole
import com.enol.salesforze.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ManagerDashboardView(
    uiState: DashboardUiState,
    onFilterChanged: (String) -> Unit,
    onOpenApprovalCenter: () -> Unit,
    onNavigateToGisMap: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAdminPortal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getNumberInstance(Locale.US)
    val hierarchyLevels = listOf("National", "North Zone", "Central Zone", "South Zone", "Karachi Area", "Lahore Area")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(grid_4),
        verticalArrangement = Arrangement.spacedBy(grid_4)
    ) {
        // --- 1. Manager Command Header ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_3),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_2)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_4)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.currentUser?.fullName ?: "Executive Dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hierarchy Scope: ${uiState.selectedHierarchyFilter}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSteelLight
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = LubricantAmber.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = uiState.userRole.name,
                                modifier = Modifier.padding(horizontal = grid_2, vertical = grid_0_5),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LubricantAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_3))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(grid_2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(hierarchyLevels) { level ->
                            val isSelected = uiState.selectedHierarchyFilter == level
                            Surface(
                                shape = RoundedCornerShape(grid_4),
                                color = if (isSelected) LubricantAmber else SurfaceContainerDark,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(grid_4))
                                    .clickable { onFilterChanged(level) }
                            ) {
                                Text(
                                    text = level,
                                    modifier = Modifier.padding(horizontal = grid_3, vertical = grid_1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BackgroundDark else OnSurfaceDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Live Team Status Ticker ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_3),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_4)
                ) {
                    Text(
                        text = "Team Field Deployment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )

                    Spacer(modifier = Modifier.height(grid_3))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(grid_2)
                    ) {
                        TeamTickerCard(
                            label = "On Field",
                            count = uiState.teamTicker.onFieldCount,
                            badgeColor = LubricantAmber,
                            icon = Icons.Default.DirectionsWalk,
                            modifier = Modifier.weight(1f)
                        )
                        TeamTickerCard(
                            label = "Not Started",
                            count = uiState.teamTicker.notStartedCount,
                            badgeColor = SlateSteelLight,
                            icon = Icons.Default.Schedule,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(grid_2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(grid_2)
                    ) {
                        TeamTickerCard(
                            label = "GPS Flagged",
                            count = uiState.teamTicker.mockGpsFlaggedCount,
                            badgeColor = LubricantAmber,
                            icon = Icons.Default.Warning,
                            modifier = Modifier.weight(1f)
                        )
                        TeamTickerCard(
                            label = "Shift Ended",
                            count = uiState.teamTicker.shiftEndedCount,
                            badgeColor = SlateSteelLight,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- 3. One-Click Approval Center Launcher Banner ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApprovalCenter() },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_3),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.totalPendingApprovals > 0) LubricantAmber else SlateSteelDark
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_2)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(grid_4),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LubricantAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = LubricantAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(grid_3))

                        Column {
                            Text(
                                text = "Pending Approvals",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${uiState.pendingCreditHoldOrders.size} Orders • ${uiState.pendingEnrollmentClients.size} Dealers • ${uiState.pendingTadaClaims.size} Claims",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSteelLight
                            )
                        }
                    }

                    if (uiState.totalPendingApprovals > 0) {
                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = LubricantAmber,
                            modifier = Modifier.padding(start = grid_2)
                        ) {
                            Text(
                                text = "${uiState.totalPendingApprovals} Action Required",
                                modifier = Modifier.padding(horizontal = grid_2, vertical = grid_1),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundDark
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Territory & District Performance Breakdown ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Districts (${uiState.selectedHierarchyFilter})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                TextButton(onClick = onNavigateToGisMap) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = LubricantAmber)
                    Spacer(modifier = Modifier.width(grid_1))
                    Text("View Map", color = LubricantAmber, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (uiState.districts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(grid_2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_4),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No territory districts configured.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )
                    }
                }
            }
        } else {
            items(uiState.districts.take(4)) { district ->
                DistrictSummaryCard(district = district, onNavigateToGisMap = onNavigateToGisMap)
            }
        }

        // --- 5. Executive Governance Shortcuts ---
        item {
            Text(
                text = "System Governance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark,
                modifier = Modifier.padding(vertical = grid_1)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_3)
            ) {
                ManagerActionCard(
                    title = "Leaderboards",
                    subtitle = "Sales Rankings",
                    icon = Icons.Default.EmojiEvents,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToLeaderboard,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.userRole == UserRole.ADMIN || uiState.userRole == UserRole.DIRECTOR) {
                    ManagerActionCard(
                        title = "API Governance",
                        subtitle = "Keys & Webhooks",
                        icon = Icons.Default.Key,
                        iconColor = LubricantAmber,
                        onClick = onNavigateToAdminPortal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(grid_6))
        }
    }
}

@Composable
private fun TeamTickerCard(
    label: String,
    count: Int,
    badgeColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(grid_2),
        color = SurfaceContainerDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark)
    ) {
        Row(
            modifier = Modifier.padding(grid_2_5),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(grid_1_5))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight
                )
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun DistrictSummaryCard(
    district: DistrictEntity,
    onNavigateToGisMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToGisMap() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(grid_2),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation_1)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(grid_3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = district.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Text(
                    text = "Province: ${district.province} | Code: ${district.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateSteelLight
                )
            }

            Surface(
                shape = RoundedCornerShape(grid_1_5),
                color = LubricantAmber.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.4f))
            ) {
                Text(
                    text = if (district.assignedAsmId != null) "Assigned" else "Vacant",
                    modifier = Modifier.padding(horizontal = grid_2, vertical = grid_0_5),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (district.assignedAsmId != null) LubricantAmber else SlateSteelLight
                )
            }
        }
    }
}

@Composable
private fun ManagerActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(grid_3))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(grid_3),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation_1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(grid_3)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(grid_2))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OnSurfaceDark)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = SlateSteelLight)
        }
    }
}

val grid_2_5 = 10.dp
