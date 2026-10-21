package com.enol.salesforze

import com.enol.salesforze.ui.theme.EnolSalesforzeTheme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import com.enol.salesforze.ui.attendance.EndShiftScreen
import com.enol.salesforze.ui.attendance.StartShiftScreen
import com.enol.salesforze.ui.enrollment.ClientEnrollmentScreen
import com.enol.salesforze.ui.enrollment.MechanicEnrollmentScreen
import com.enol.salesforze.ui.gis.DistrictMapScreen
import com.enol.salesforze.ui.order.OrderCartScreen
import com.enol.salesforze.ui.visit.VisitExecutionScreen
import com.enol.salesforze.ui.dashboard.DashboardScreen
import com.enol.salesforze.ui.leaderboard.LeaderboardScreen
import com.enol.salesforze.ui.admin.AdminApiPortalScreen
import com.enol.salesforze.ui.tada.TadaClaimScreen
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.LubricantAmber
import com.enol.salesforze.ui.theme.SlateSteel
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.spacing
import com.enol.salesforze.ui.theme.statusColors
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Key
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint

enum class AppNavigationTab(val title: String) {
    DASHBOARD("Dashboard"),
    GIS_MAP("GIS Map"),
    ORDER_CART("Order Cart"),
    VISIT_EXECUTION("Visit Check-In"),
    CLIENT_ENROLL("New Customer"),
    MECHANIC_ENROLL("New Mechanic"),
    START_SHIFT("Start Day"),
    END_SHIFT("End Day"),
    TADA_CLAIM("TADA Expense"),
    LEADERBOARD("Sales Races"),
    ADMIN_PORTAL("API Governance")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            var highContrastMode by remember { mutableStateOf(false) }
            var currentTab by remember { mutableStateOf(AppNavigationTab.DASHBOARD) }

            EnolSalesforzeTheme(
                darkTheme = darkTheme,
                highContrast = highContrastMode
            ) {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = EnolNavy,
                            contentColor = Color.White
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppNavigationTab.DASHBOARD,
                                onClick = { currentTab = AppNavigationTab.DASHBOARD },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text(AppNavigationTab.DASHBOARD.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EnolNavy,
                                    selectedTextColor = LubricantAmber,
                                    indicatorColor = LubricantAmber,
                                    unselectedIconColor = SlateSteelLight,
                                    unselectedTextColor = SlateSteelLight
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == AppNavigationTab.GIS_MAP,
                                onClick = { currentTab = AppNavigationTab.GIS_MAP },
                                icon = { Icon(Icons.Default.Map, contentDescription = "GIS Map") },
                                label = { Text(AppNavigationTab.GIS_MAP.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EnolNavy,
                                    selectedTextColor = LubricantAmber,
                                    indicatorColor = LubricantAmber,
                                    unselectedIconColor = SlateSteelLight,
                                    unselectedTextColor = SlateSteelLight
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == AppNavigationTab.ORDER_CART,
                                onClick = { currentTab = AppNavigationTab.ORDER_CART },
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Order Cart") },
                                label = { Text(AppNavigationTab.ORDER_CART.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EnolNavy,
                                    selectedTextColor = LubricantAmber,
                                    indicatorColor = LubricantAmber,
                                    unselectedIconColor = SlateSteelLight,
                                    unselectedTextColor = SlateSteelLight
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == AppNavigationTab.TADA_CLAIM,
                                onClick = { currentTab = AppNavigationTab.TADA_CLAIM },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "TADA") },
                                label = { Text(AppNavigationTab.TADA_CLAIM.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EnolNavy,
                                    selectedTextColor = LubricantAmber,
                                    indicatorColor = LubricantAmber,
                                    unselectedIconColor = SlateSteelLight,
                                    unselectedTextColor = SlateSteelLight
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == AppNavigationTab.LEADERBOARD,
                                onClick = { currentTab = AppNavigationTab.LEADERBOARD },
                                icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Sales Races") },
                                label = { Text(AppNavigationTab.LEADERBOARD.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EnolNavy,
                                    selectedTextColor = LubricantAmber,
                                    indicatorColor = LubricantAmber,
                                    unselectedIconColor = SlateSteelLight,
                                    unselectedTextColor = SlateSteelLight
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            AppNavigationTab.DASHBOARD -> DashboardScreen(
                                onNavigateToOrderCart = { currentTab = AppNavigationTab.ORDER_CART },
                                onNavigateToVisitExecution = { currentTab = AppNavigationTab.VISIT_EXECUTION },
                                onNavigateToClientEnroll = { currentTab = AppNavigationTab.CLIENT_ENROLL },
                                onNavigateToMechanicEnroll = { currentTab = AppNavigationTab.MECHANIC_ENROLL },
                                onNavigateToGisMap = { currentTab = AppNavigationTab.GIS_MAP },
                                onNavigateToTada = { currentTab = AppNavigationTab.TADA_CLAIM },
                                onNavigateToLeaderboard = { currentTab = AppNavigationTab.LEADERBOARD },
                                onNavigateToAdminPortal = { currentTab = AppNavigationTab.ADMIN_PORTAL },
                                onNavigateToStartShift = { currentTab = AppNavigationTab.START_SHIFT },
                                onNavigateToEndShift = { currentTab = AppNavigationTab.END_SHIFT }
                            )
                            AppNavigationTab.ORDER_CART -> OrderCartScreen(
                                onOrderPlaced = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.VISIT_EXECUTION -> VisitExecutionScreen(
                                onNavigateToOrderCart = { currentTab = AppNavigationTab.ORDER_CART },
                                onVisitCompleted = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.CLIENT_ENROLL -> ClientEnrollmentScreen(
                                onClientEnrolled = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.MECHANIC_ENROLL -> MechanicEnrollmentScreen(
                                onMechanicEnrolled = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.GIS_MAP -> DistrictMapScreen(
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.START_SHIFT -> StartShiftScreen(
                                onShiftStarted = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.END_SHIFT -> EndShiftScreen(
                                onShiftEnded = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.TADA_CLAIM -> TadaClaimScreen(
                                onClaimSubmitted = { currentTab = AppNavigationTab.DASHBOARD },
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.LEADERBOARD -> LeaderboardScreen(
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                            AppNavigationTab.ADMIN_PORTAL -> AdminApiPortalScreen(
                                onNavigateBack = { currentTab = AppNavigationTab.DASHBOARD }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndustrialDashboardScreen(
    isDarkTheme: Boolean = false,
    isHighContrast: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onToggleContrast: () -> Unit = {},
    onNavigateToGis: () -> Unit = {},
    onNavigateToShift: () -> Unit = {},
    onNavigateToOrderCart: () -> Unit = {},
    onNavigateToVisit: () -> Unit = {},
    onNavigateToClientEnroll: () -> Unit = {},
    onNavigateToMechanicEnroll: () -> Unit = {}
) {
    val spacing = MaterialTheme.spacing
    val statusColors = MaterialTheme.statusColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ENOL SALESFORZE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "FIELD AUTOMATION & LUBRICANT DISTRIBUTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                },
                actions = {
                    // Outdoor High-Contrast Mode Toggle (for glaring sunlight)
                    IconButton(
                        onClick = onToggleContrast,
                        modifier = Modifier.size(spacing.touchTargetMin)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessHigh,
                            contentDescription = "Toggle Outdoor Sunlight High-Contrast",
                            tint = if (isHighContrast) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Light / Dark Theme Toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.size(spacing.touchTargetMin)
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.defaultScreenPadding, vertical = spacing.grid3),
            verticalArrangement = Arrangement.spacedBy(spacing.grid3)
        ) {
            // Field Status Chip & Route Bar
            FieldOperationalStatusRow(
                isHighContrast = isHighContrast
            )

            // High Contrast Notice Banner (When Active)
            if (isHighContrast) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.grid2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(spacing.grid5)
                        )
                        Spacer(modifier = Modifier.width(spacing.grid2))
                        Text(
                            text = "OUTDOOR HIGH-CONTRAST MODE ACTIVE (FIELD SUNLIGHT OPTIMIZED)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Quick Stats Metric Cards
            Text(
                text = "TODAY'S SHIFT SUMMARY",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.grid2)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Liters Booked",
                    value = "4,850 L",
                    subtitle = "Target: 6,000 L (80.8%)",
                    icon = Icons.Default.LocalGasStation,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    progress = 0.808f
                )

                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending Visits",
                    value = "6 / 14",
                    subtitle = "8 Shops Visited",
                    icon = Icons.Default.Storefront,
                    iconTint = MaterialTheme.colorScheme.primary,
                    progress = 8f / 14f
                )
            }

            // Recovery PKR Metric Card (Full Width)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(spacing.cardContentPadding)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(statusColors.successContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = statusColors.onSuccessContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(spacing.grid2))
                            Column {
                                Text(
                                    text = "Recovery PKR Collected",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "PKR 485,000",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = statusColors.successContainer
                        ) {
                            Text(
                                text = "92% OF TARGET",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColors.onSuccessContainer,
                                modifier = Modifier.padding(horizontal = spacing.grid2, vertical = spacing.grid1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.grid2))
                    LinearProgressIndicator(
                        progress = { 0.92f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spacing.grid1_5)
                            .clip(MaterialTheme.shapes.extraSmall),
                        color = statusColors.success,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.grid1))
                    Text(
                        text = "Pending Collection from 3 Customers: PKR 120,000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Actions Bar
            Text(
                text = "FIELD OPERATIONS DISPATCH",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.grid2)
            ) {
                Button(
                    onClick = onNavigateToOrderCart,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(spacing.touchTargetMin)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(text = "Book Order", style = MaterialTheme.typography.labelLarge)
                }

                FilledTonalButton(
                    onClick = onNavigateToVisit,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(spacing.touchTargetMin)
                ) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(text = "Check-In", style = MaterialTheme.typography.labelLarge)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.grid2)
            ) {
                OutlinedButton(
                    onClick = onNavigateToGis,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(spacing.touchTargetMin)
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(text = "Territory GIS", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onNavigateToShift,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(spacing.touchTargetMin)
                ) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(text = "Start Field Day", style = MaterialTheme.typography.labelLarge)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.grid2)
            ) {
                OutlinedButton(
                    onClick = onNavigateToClientEnroll,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(spacing.touchTargetMin)
                ) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(text = "New Customer", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onNavigateToMechanicEnroll,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .height(spacing.touchTargetMin)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(text = "New Mechanic", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Route & Customer Visit Queue Preview
            Text(
                text = "UPCOMING VISITS ON ROUTE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            VisitItemCard(
                shopName = "Al-Madina Auto Workshop & Oil Service",
                ownerName = "Muhammad Tariq (Proprietor)",
                address = "Shop #12, Badami Bagh Auto Market, Lahore",
                distance = "180 m away",
                outstandingBalance = "PKR 45,000",
                isNext = true
            )

            VisitItemCard(
                shopName = "Ravi Lubricants & Filter House",
                ownerName = "Haji Usman",
                address = "Circular Road Near Bhati Gate, Lahore",
                distance = "1.2 km away",
                outstandingBalance = "PKR 0 (Clear)",
                isNext = false
            )

            Spacer(modifier = Modifier.height(spacing.grid4))
        }
    }
}

@Composable
fun FieldOperationalStatusRow(
    isHighContrast: Boolean
) {
    val spacing = MaterialTheme.spacing
    val statusColors = MaterialTheme.statusColors

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            if (isHighContrast) 2.dp else 1.dp,
            MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.grid3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColors.success)
                )
                Spacer(modifier = Modifier.width(spacing.grid2))
                Column {
                    Text(
                        text = "ONLINE • POSTGIS SYNCED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColors.success
                    )
                    Text(
                        text = "Route #04 • Badami Bagh North",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Status",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.grid1))
                Text(
                    text = "0 Pending Sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    progress: Float
) {
    val spacing = MaterialTheme.spacing

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardContentPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(spacing.grid1))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(spacing.grid1_5))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.grid1)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = iconTint,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(spacing.grid1))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VisitItemCard(
    shopName: String,
    ownerName: String,
    address: String,
    distance: String,
    outstandingBalance: String,
    isNext: Boolean
) {
    val spacing = MaterialTheme.spacing
    val statusColors = MaterialTheme.statusColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isNext) 2.dp else 1.dp,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.cardContentPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = shopName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (isNext) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "NEXT VISIT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = spacing.grid2, vertical = spacing.grid0_5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.grid0_5))
            Text(
                text = ownerName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(spacing.grid2))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(spacing.grid2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.grid1))
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "Bal: $outstandingBalance",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (outstandingBalance.contains("PKR 0")) statusColors.success else statusColors.warning
                )
            }
        }
    }
}

@Preview(name = "Light Mode - Industrial Theme", showBackground = true)
@Composable
fun DashboardLightPreview() {
    EnolSalesforzeTheme(darkTheme = false, highContrast = false) {
        IndustrialDashboardScreen()
    }
}

@Preview(name = "Dark Mode - Industrial Theme", showBackground = true)
@Composable
fun DashboardDarkPreview() {
    EnolSalesforzeTheme(darkTheme = true, highContrast = false) {
        IndustrialDashboardScreen(isDarkTheme = true)
    }
}

@Preview(name = "High-Contrast Outdoor Mode", showBackground = true)
@Composable
fun DashboardHighContrastPreview() {
    EnolSalesforzeTheme(darkTheme = false, highContrast = true) {
        IndustrialDashboardScreen(isHighContrast = true)
    }
}
