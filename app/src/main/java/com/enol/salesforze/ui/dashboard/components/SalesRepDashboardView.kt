package com.enol.salesforze.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enol.salesforze.data.local.entities.CompetitionEntity
import com.enol.salesforze.data.local.entities.TadaClaimEntity
import com.enol.salesforze.ui.dashboard.DashboardUiState
import com.enol.salesforze.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SalesRepDashboardView(
    uiState: DashboardUiState,
    onNavigateToOrderCart: () -> Unit,
    onNavigateToVisitExecution: () -> Unit,
    onNavigateToClientEnroll: () -> Unit,
    onNavigateToMechanicEnroll: () -> Unit,
    onNavigateToGisMap: () -> Unit,
    onNavigateToTada: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToStartShift: () -> Unit,
    onNavigateToEndShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getNumberInstance(Locale.US)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding = PaddingValues(grid_4),
        verticalArrangement = Arrangement.spacedBy(grid_4)
    ) {
        // --- 1. Rep Profile & Territory Header ---
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
                                text = "Welcome, ${uiState.currentUser?.fullName ?: "Sales Officer"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Spacer(modifier = Modifier.height(grid_0_5))
                            Text(
                                text = "Territory: Karachi South & Korangi Industrial Beat",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSteelLight
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(grid_2),
                            color = EnolNavyMuted.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber)
                        ) {
                            Text(
                                text = "SALES REP",
                                modifier = Modifier.padding(horizontal = grid_2, vertical = grid_1),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LubricantAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_3))

                    // Shift Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(grid_2)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToStartShift,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(grid_2),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LubricantAmber
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(grid_1))
                            Text("Start Shift", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onNavigateToEndShift,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(grid_2),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SlateSteelLight
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(grid_1))
                            Text("End Shift", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // --- 2. Today's Beat Progress Card ---
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = LubricantAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(grid_2))
                            Text(
                                text = "Today's Beat Execution",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                        }
                        Text(
                            text = "${(uiState.beatProgressPct * 100).toInt()}% Done",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LubricantAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(grid_2))

                    LinearProgressIndicator(
                        progress = { uiState.beatProgressPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = LubricantAmber,
                        trackColor = SlateSteelDark
                    )

                    Spacer(modifier = Modifier.height(grid_2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Visits: ${uiState.todayVisitsCompleted} / ${uiState.todayVisitsPlanned}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )
                        val remaining = (uiState.todayVisitsPlanned - uiState.todayVisitsCompleted).coerceAtLeast(0)
                        Text(
                            text = "$remaining Remaining",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceDark
                        )
                    }
                }
            }
        }

        // --- 3. Monthly Target vs Achieved KPI Gauges ---
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
                        .padding(grid_4),
                    verticalArrangement = Arrangement.spacedBy(grid_3)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Targets (MTD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = LubricantAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Gauge 1: Volume (Liters)
                    KpiProgressRow(
                        title = "Volume",
                        achievedText = "${currencyFormatter.format(uiState.achievedVolumeLiters.toInt())} L",
                        targetText = "${currencyFormatter.format(uiState.targetVolumeLiters.toInt())} L",
                        progress = uiState.volumeProgressPct,
                        progressColor = LubricantAmber
                    )

                    // Gauge 2: Revenue (PKR)
                    KpiProgressRow(
                        title = "Revenue",
                        achievedText = "PKR ${currencyFormatter.format(uiState.achievedRevenuePkr.toInt())}",
                        targetText = "PKR ${currencyFormatter.format(uiState.targetRevenuePkr.toInt())}",
                        progress = uiState.revenueProgressPct,
                        progressColor = LubricantAmber
                    )

                    // Gauge 3: Visit Target
                    val visitProgress = if (uiState.targetVisits > 0) (uiState.achievedVisits.toFloat() / uiState.targetVisits).coerceIn(0f, 1f) else 0f
                    KpiProgressRow(
                        title = "Visits",
                        achievedText = "${uiState.achievedVisits}",
                        targetText = "${uiState.targetVisits}",
                        progress = visitProgress,
                        progressColor = LubricantAmber
                    )

                    // Gauge 4: New Client Onboardings
                    val clientProgress = if (uiState.targetNewClients > 0) (uiState.achievedNewClients.toFloat() / uiState.targetNewClients).coerceIn(0f, 1f) else 0f
                    KpiProgressRow(
                        title = "New Dealers",
                        achievedText = "${uiState.achievedNewClients}",
                        targetText = "${uiState.targetNewClients}",
                        progress = clientProgress,
                        progressColor = LubricantAmber
                    )
                }
            }
        }

        // --- 4. Active Competition Leaderboard Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLeaderboard() },
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(grid_3),
                border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.5f)),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = LubricantAmber.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = LubricantAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(grid_2))
                            Column {
                                Text(
                                    text = "Sales Competition",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceDark
                                )
                                Text(
                                    text = "Grand Synthetics Sprint 2026",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LubricantAmber
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(grid_1),
                            color = LubricantAmber,
                            modifier = Modifier.padding(start = grid_2)
                        ) {
                            Text(
                                text = "RANK #3",
                                modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BackgroundDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_3))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Points", style = MaterialTheme.typography.labelSmall, color = SlateSteelLight)
                            Text(text = "12,450 pts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Prize Pool", style = MaterialTheme.typography.labelSmall, color = SlateSteelLight)
                            Text(text = "Honda 125cc + PKR 50K", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurfaceDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Leaderboard",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LubricantAmber
                        )
                        Spacer(modifier = Modifier.width(grid_1))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = LubricantAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- 5. Quick Field Operations Grid ---
        item {
            Text(
                text = "Field Operations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark,
                modifier = Modifier.padding(vertical = grid_1)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_3)
            ) {
                QuickActionCard(
                    title = "Book Order",
                    subtitle = "Catalog & Cart",
                    icon = Icons.Default.ShoppingCart,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToOrderCart,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Check In",
                    subtitle = "Geofenced Visit",
                    icon = Icons.Default.LocationOn,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToVisitExecution,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(grid_3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_3)
            ) {
                QuickActionCard(
                    title = "New Dealer",
                    subtitle = "Shop Onboarding",
                    icon = Icons.Default.Storefront,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToClientEnroll,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "New Mechanic",
                    subtitle = "Loyalty Onboarding",
                    icon = Icons.Default.Build,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToMechanicEnroll,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(grid_3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(grid_3)
            ) {
                QuickActionCard(
                    title = "Territory Map",
                    subtitle = "District Routes",
                    icon = Icons.Default.Map,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToGisMap,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Expense Claim",
                    subtitle = "Daily TADA",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = LubricantAmber,
                    onClick = onNavigateToTada,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- 6. Recent TADA Expense Claims ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent TADA Claims",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                TextButton(onClick = onNavigateToTada) {
                    Text("File Claim", color = LubricantAmber, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (uiState.recentTadaClaims.isEmpty()) {
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
                            text = "No recent expense claims recorded for this shift cycle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )
                    }
                }
            }
        } else {
            items(uiState.recentTadaClaims.take(3)) { claim ->
                TadaClaimSummaryCard(claim = claim)
            }
        }

        item {
            Spacer(modifier = Modifier.height(grid_6))
        }
    }
}

@Composable
private fun KpiProgressRow(
    title: String,
    achievedText: String,
    targetText: String,
    progress: Float,
    progressColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = SlateSteelLight)
            Text(
                text = "$achievedText / $targetText (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark
            )
        }
        Spacer(modifier = Modifier.height(grid_1))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = SlateSteelDark
        )
    }
}

@Composable
private fun QuickActionCard(
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

@Composable
private fun TadaClaimSummaryCard(claim: TadaClaimEntity) {
    val currencyFormatter = NumberFormat.getNumberInstance(Locale.US)
    val statusColor = if (claim.status.equals("APPROVED", ignoreCase = true)) LubricantAmber else SlateSteelLight

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(grid_2)
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
                    text = "Claim: ${claim.claimDate}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Text(
                    text = "Distance: ${"%.1f".format(claim.odometerDeltaKm)} km | Daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateSteelLight
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PKR ${currencyFormatter.format(claim.totalClaimAmount.toInt())}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Surface(
                    shape = RoundedCornerShape(grid_1),
                    color = statusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = claim.status,
                        modifier = Modifier.padding(horizontal = grid_1_5, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}
