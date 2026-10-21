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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
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
