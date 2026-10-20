package com.enol.salesforze.ui.leaderboard

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.data.remote.models.CompetitionScoreDto
import com.enol.salesforze.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRulesDialog by remember { mutableStateOf(false) }
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sales Races & Leaderboards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "National Sales Gamification & Trophies",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateSteelLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurfaceDark)
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesDialog = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Rules", tint = LubricantAmber)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(grid_4),
            verticalArrangement = Arrangement.spacedBy(grid_4)
        ) {
            // --- 1. Active Competitions Selector ---
            item {
                Text(
                    text = "Active Sales Competitions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateSteelLight
                )
                Spacer(modifier = Modifier.height(grid_1_5))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(grid_2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.activeCompetitions) { comp ->
                        val isSelected = comp.id == uiState.selectedCompetition?.id
                        Surface(
                            shape = RoundedCornerShape(grid_3),
                            color = if (isSelected) LubricantAmber else SurfaceContainerDark,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
                            modifier = Modifier
                                .clip(RoundedCornerShape(grid_3))
                                .clickable { viewModel.selectCompetition(comp) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = grid_3, vertical = grid_2),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (isSelected) EnolNavyDark else LubricantAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(grid_1_5))
                                Text(
                                    text = comp.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EnolNavyDark else OnSurfaceDark
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. Competition Details & Grand Prize Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(grid_3),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.5f))
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
                            Text(
                                text = uiState.selectedCompetition?.title ?: "Grand Synthetics Sprint",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Surface(
                                shape = RoundedCornerShape(grid_2),
                                color = SuccessEmerald.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessEmerald)
                            ) {
                                Text(
                                    text = "ACTIVE RACE",
                                    modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessEmerald
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(grid_1))

                        Text(
                            text = uiState.selectedCompetition?.description ?: "Highest Volume in 100% Synthetic PCMO",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )

                        Spacer(modifier = Modifier.height(grid_3))

                        Surface(
                            shape = RoundedCornerShape(grid_2),
                            color = SurfaceContainerDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(grid_3),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = LubricantAmber.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CardGiftcard,
                                            contentDescription = null,
                                            tint = LubricantAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(grid_2_5))

                                Column {
                                    Text(
                                        text = "Top Prize Pool",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateSteelLight
                                    )
                                    Text(
                                        text = "1st: Honda CD 70cc + PKR 50K | 2nd: PKR 75K | 3rd: PKR 35K",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. Top-3 Podium Layout ---
            item {
                Text(
                    text = "The Podium (Top 3 Contenders)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
                Spacer(modifier = Modifier.height(grid_2))

                val top3 = uiState.leaderboardEntries.take(3)
                if (top3.size >= 3) {
                    PodiumSection(top3 = top3)
                }
            }

            // --- 4. Current User Standing Banner ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHighDark),
                    shape = RoundedCornerShape(grid_3),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_3),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = LubricantAmber,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "#${uiState.currentUserRank}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EnolNavyDark,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(grid_2_5))

                            Column {
                                Text(
                                    text = "Your Standing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceDark
                                )
                                Text(
                                    text = "${numberFormatter.format(uiState.currentUserPoints)} Points Accumulated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LubricantAmber
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(grid_2),
                            color = EnolNavyLight
                        ) {
                            Text(
                                text = "Eligible for 3rd Prize",
                                modifier = Modifier.padding(horizontal = grid_2, vertical = grid_1),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessEmerald
                            )
                        }
                    }
                }
            }

            // --- 5. Full Standings Table (#4 and onwards) ---
            item {
                Text(
                    text = "Full National Rankings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceDark
                )
            }

            val remainingEntries = uiState.leaderboardEntries.drop(3)
            items(remainingEntries) { entry ->
                LeaderboardRankRow(entry = entry)
            }

            item {
                Spacer(modifier = Modifier.height(grid_6))
            }
        }
    }

    // Rules Dialog
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Text("Competition Scoring Rules", fontWeight = FontWeight.Bold, color = OnSurfaceDark)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(grid_2)) {
                    Text(
                        text = "Points are automatically calculated via the backend scoring engine:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateSteelLight
                    )
                    RuleItem(icon = Icons.Default.LocalGasStation, rule = "10 Points per Liter of Synthetic Oil Booked")
                    RuleItem(icon = Icons.Default.Storefront, rule = "250 Points per Verified New Dealer Enrolled")
                    RuleItem(icon = Icons.Default.Build, rule = "100 Points per Active Mechanic Onboarded")
                    RuleItem(icon = Icons.Default.CheckCircle, rule = "50 Points per Geofenced Verified Visit")
                    RuleItem(icon = Icons.Default.Payments, rule = "5 Points per PKR 1,000 Cash Recovery Slip")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRulesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber)
                ) {
                    Text("Got It", color = EnolNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

// =========================================================================
// PODIUM COMPONENT
// =========================================================================

@Composable
private fun PodiumSection(top3: List<CompetitionScoreDto>) {
    val first = top3[0]
    val second = top3[1]
    val third = top3[2]

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(grid_2),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place (Silver)
        PodiumStep(
            rank = 2,
            repName = second.userName ?: "Rep #2",
            points = second.points,
            color = Color(0xFFC0C0C0),
            height = 120.dp,
            modifier = Modifier.weight(1f)
        )

        // 1st Place (Gold)
        PodiumStep(
            rank = 1,
            repName = first.userName ?: "Rep #1",
            points = first.points,
            color = Color(0xFFFFD700),
            height = 150.dp,
            modifier = Modifier.weight(1.15f)
        )

        // 3rd Place (Bronze)
        PodiumStep(
            rank = 3,
            repName = third.userName ?: "Rep #3",
            points = third.points,
            color = Color(0xFFCD7F32),
            height = 100.dp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PodiumStep(
    rank: Int,
    repName: String,
    points: Int,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Medal / Crown Icon
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, color),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(grid_1))

        Text(
            text = repName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceDark,
            maxLines = 1
        )
        Text(
            text = "${numberFormatter.format(points)} pts",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )

        Spacer(modifier = Modifier.height(grid_1))

        Surface(
            shape = RoundedCornerShape(topStart = grid_2, topEnd = grid_2),
            color = SurfaceContainerDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when (rank) {
                        1 -> "GOLD"
                        2 -> "SILVER"
                        else -> "BRONZE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRankRow(entry: CompetitionScoreDto) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.US)

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SlateSteelDark,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${entry.rank ?: "-"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(grid_2_5))

                Column {
                    Text(
                        text = entry.userName ?: "Sales Officer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = "Metric Value: ${"%.0f".format(entry.currentMetricValue)} units",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateSteelLight
                    )
                }
            }

            Text(
                text = "${numberFormatter.format(entry.points)} pts",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = LubricantAmber
            )
        }
    }
}

@Composable
private fun RuleItem(icon: androidx.compose.ui.graphics.vector.ImageVector, rule: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = LubricantAmber, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(grid_1_5))
        Text(text = rule, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDark)
    }
}
