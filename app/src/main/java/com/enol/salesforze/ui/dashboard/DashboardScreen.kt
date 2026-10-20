package com.enol.salesforze.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.ui.dashboard.components.ApprovalCenterBottomSheet
import com.enol.salesforze.ui.dashboard.components.ManagerDashboardView
import com.enol.salesforze.ui.dashboard.components.SalesRepDashboardView
import com.enol.salesforze.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToOrderCart: () -> Unit,
    onNavigateToVisitExecution: () -> Unit,
    onNavigateToClientEnroll: () -> Unit,
    onNavigateToMechanicEnroll: () -> Unit,
    onNavigateToGisMap: () -> Unit,
    onNavigateToTada: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAdminPortal: () -> Unit,
    onNavigateToStartShift: () -> Unit,
    onNavigateToEndShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var adminPreviewAsRep by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = LubricantAmber,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "E",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EnolNavyDark,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(grid_2))

                        Column {
                            Text(
                                text = "ENOL SALESFORZE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Text(
                                text = "Automotive Lubricants & Field Force",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateSteelLight
                            )
                        }
                    }
                },
                actions = {
                    // Admin Role Switcher (for previewing both Rep and Executive views)
                    if (uiState.userRole == UserRole.ADMIN) {
                        TextButton(onClick = { adminPreviewAsRep = !adminPreviewAsRep }) {
                            Text(
                                text = if (adminPreviewAsRep) "Switch: Exec" else "Switch: Rep",
                                style = MaterialTheme.typography.labelSmall,
                                color = LubricantAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.loadDashboardData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Telemetry",
                            tint = SlateSteelLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = OnSurfaceDark
                )
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val showManagerView = uiState.isManagerOrDirector && !adminPreviewAsRep

            if (showManagerView) {
                ManagerDashboardView(
                    uiState = uiState,
                    onFilterChanged = { viewModel.setHierarchyFilter(it) },
                    onOpenApprovalCenter = { viewModel.toggleApprovalCenter(true) },
                    onNavigateToGisMap = onNavigateToGisMap,
                    onNavigateToLeaderboard = onNavigateToLeaderboard,
                    onNavigateToAdminPortal = onNavigateToAdminPortal
                )
            } else {
                SalesRepDashboardView(
                    uiState = uiState,
                    onNavigateToOrderCart = onNavigateToOrderCart,
                    onNavigateToVisitExecution = onNavigateToVisitExecution,
                    onNavigateToClientEnroll = onNavigateToClientEnroll,
                    onNavigateToMechanicEnroll = onNavigateToMechanicEnroll,
                    onNavigateToGisMap = onNavigateToGisMap,
                    onNavigateToTada = onNavigateToTada,
                    onNavigateToLeaderboard = onNavigateToLeaderboard,
                    onNavigateToStartShift = onNavigateToStartShift,
                    onNavigateToEndShift = onNavigateToEndShift
                )
            }

            // Executive One-Click Approval Center Modal Sheet
            if (uiState.isApprovalCenterOpen) {
                ApprovalCenterBottomSheet(
                    uiState = uiState,
                    onDismiss = { viewModel.toggleApprovalCenter(false) },
                    onApproveOrder = { viewModel.approveCreditHoldOrder(it) },
                    onRejectOrder = { viewModel.rejectCreditHoldOrder(it) },
                    onApproveClient = { viewModel.approveClientEnrollment(it) },
                    onRejectClient = { viewModel.rejectClientEnrollment(it) },
                    onApproveTada = { viewModel.approveTadaClaim(it) },
                    onRejectTada = { viewModel.rejectTadaClaim(it) }
                )
            }
        }
    }
}
