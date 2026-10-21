package com.enol.salesforze.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enol.salesforze.data.local.entities.ClientEntity
import com.enol.salesforze.data.local.entities.OrderEntity
import com.enol.salesforze.data.local.entities.TadaClaimEntity
import com.enol.salesforze.ui.dashboard.DashboardUiState
import com.enol.salesforze.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalCenterBottomSheet(
    uiState: DashboardUiState,
    onDismiss: () -> Unit,
    onApproveOrder: (String) -> Unit,
    onRejectOrder: (String) -> Unit,
    onApproveClient: (String) -> Unit,
    onRejectClient: (String) -> Unit,
    onApproveTada: (String) -> Unit,
    onRejectTada: (String) -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Credit Holds (${uiState.pendingCreditHoldOrders.size})",
        "Dealers (${uiState.pendingEnrollmentClients.size})",
        "TADA (${uiState.pendingTadaClaims.size})"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = grid_2)
                    .width(48.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = SlateSteelDark
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = grid_4)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = LubricantAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(
                        text = "Executive Approval Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SlateSteelLight)
                }
            }

            Spacer(modifier = Modifier.height(grid_2))

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceContainerDark,
                contentColor = LubricantAmber,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) LubricantAmber else SlateSteelLight
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(grid_3))

            // Tab Content
            when (selectedTab) {
                0 -> CreditHoldsTabContent(
                    orders = uiState.pendingCreditHoldOrders,
                    onApprove = onApproveOrder,
                    onReject = onRejectOrder
                )
                1 -> ClientEnrollmentsTabContent(
                    clients = uiState.pendingEnrollmentClients,
                    onApprove = onApproveClient,
                    onReject = onRejectClient
                )
                2 -> TadaClaimsTabContent(
                    claims = uiState.pendingTadaClaims,
                    onApprove = onApproveTada,
                    onReject = onRejectTada
                )
            }
        }
    }
}

// =========================================================================
// 1. CREDIT HOLDS TAB
// =========================================================================

@Composable
private fun CreditHoldsTabContent(
    orders: List<OrderEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val currencyFormatter = NumberFormat.getNumberInstance(Locale.US)

    if (orders.isEmpty()) {
        EmptyApprovalView(message = "All credit hold orders have been cleared and released.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(grid_3),
            contentPadding = PaddingValues(bottom = grid_6)
        ) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                    shape = RoundedCornerShape(grid_2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_3)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Order #${order.orderNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Surface(
                                shape = RoundedCornerShape(grid_1),
                                color = WarningAmber.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber)
                            ) {
                                Text(
                                    text = "CREDIT HOLD",
                                    modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(grid_1))

                        Text(
                            text = "Dealer Ref: ${order.clientId} | Payment: ${order.paymentTerms}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )

                        Spacer(modifier = Modifier.height(grid_2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Order Total", style = MaterialTheme.typography.labelSmall, color = SlateSteelLight)
                                Text(
                                    text = "PKR ${currencyFormatter.format(order.netAmount.toInt())}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LubricantAmber
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(grid_2)) {
                                OutlinedButton(
                                    onClick = { onReject(order.id) },
                                    shape = RoundedCornerShape(grid_2),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateSteelLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
                                    contentPadding = PaddingValues(horizontal = grid_3, vertical = grid_1)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(grid_1))
                                    Text("Reject", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = { onApprove(order.id) },
                                    shape = RoundedCornerShape(grid_2),
                                    colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = BackgroundDark),
                                    contentPadding = PaddingValues(horizontal = grid_3, vertical = grid_1)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = BackgroundDark)
                                    Spacer(modifier = Modifier.width(grid_1))
                                    Text("Release", style = MaterialTheme.typography.labelMedium, color = BackgroundDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. CLIENT ENROLLMENTS TAB
// =========================================================================

@Composable
private fun ClientEnrollmentsTabContent(
    clients: List<ClientEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (clients.isEmpty()) {
        EmptyApprovalView(message = "All dealer & workshop onboarding requests are approved.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(grid_3),
            contentPadding = PaddingValues(bottom = grid_6)
        ) {
            items(clients) { client ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                    shape = RoundedCornerShape(grid_2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_3)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = client.businessName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Surface(
                                shape = RoundedCornerShape(grid_1),
                                color = LubricantAmber.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = client.approvalStatus,
                                    modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LubricantAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(grid_1))

                        Text(
                            text = "Proprietor: ${client.ownerName} • Phone: ${client.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )
                        Text(
                            text = "CNIC: ${client.cnic ?: "Unspecified"} • Address: ${client.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )

                        Spacer(modifier = Modifier.height(grid_2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onReject(client.id) },
                                shape = RoundedCornerShape(grid_2),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateSteelLight),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
                                contentPadding = PaddingValues(horizontal = grid_3, vertical = grid_1)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(grid_1))
                                Text("Reject", style = MaterialTheme.typography.labelMedium)
                            }

                            Spacer(modifier = Modifier.width(grid_2))

                            Button(
                                onClick = { onApprove(client.id) },
                                shape = RoundedCornerShape(grid_2),
                                colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = BackgroundDark),
                                contentPadding = PaddingValues(horizontal = grid_3, vertical = grid_1)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = BackgroundDark)
                                Spacer(modifier = Modifier.width(grid_1))
                                Text("Approve", style = MaterialTheme.typography.labelMedium, color = BackgroundDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. TADA EXPENSE CLAIMS TAB
// =========================================================================

@Composable
private fun TadaClaimsTabContent(
    claims: List<TadaClaimEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val currencyFormatter = NumberFormat.getNumberInstance(Locale.US)

    if (claims.isEmpty()) {
        EmptyApprovalView(message = "All field expense (TADA) claims have been verified and processed.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(grid_3),
            contentPadding = PaddingValues(bottom = grid_6)
        ) {
            items(claims) { claim ->
                val isFlagged = claim.variancePercentage > 10.0 || claim.status == "FLAGGED"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerDark),
                    shape = RoundedCornerShape(grid_2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFlagged) LubricantAmber else SlateSteelDark
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_3)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Claim Date: ${claim.claimDate}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )

                            Surface(
                                shape = RoundedCornerShape(grid_1),
                                color = LubricantAmber.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (isFlagged) "FLAGGED (>10%)" else "PENDING",
                                    modifier = Modifier.padding(horizontal = grid_2, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LubricantAmber
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(grid_1))

                        Text(
                            text = "Odometer: ${"%.1f".format(claim.odometerDeltaKm)} km vs GPS: ${"%.1f".format(claim.gpsDistanceKm)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )
                        Text(
                            text = "Variance: ${"%.1f".format(claim.variancePercentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight
                        )

                        Spacer(modifier = Modifier.height(grid_2))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Claim Total", style = MaterialTheme.typography.labelSmall, color = SlateSteelLight)
                                Text(
                                    text = "PKR ${currencyFormatter.format(claim.totalClaimAmount.toInt())}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LubricantAmber
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(grid_2)) {
                                OutlinedButton(
                                    onClick = { onReject(claim.id) },
                                    shape = RoundedCornerShape(grid_2),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateSteelLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
                                    contentPadding = PaddingValues(horizontal = grid_3, vertical = grid_1)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(grid_1))
                                    Text("Reject", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = { onApprove(claim.id) },
                                    shape = RoundedCornerShape(grid_2),
                                    colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = BackgroundDark),
                                    contentPadding = PaddingValues(horizontal = grid_3, vertical = grid_1)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = BackgroundDark)
                                    Spacer(modifier = Modifier.width(grid_1))
                                    Text("Approve", style = MaterialTheme.typography.labelMedium, color = BackgroundDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyApprovalView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(grid_6),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessEmerald,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(grid_3))
            Text(
                text = "Queue All Clear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceDark
            )
            Spacer(modifier = Modifier.height(grid_1))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = SlateSteelLight
            )
        }
    }
}
