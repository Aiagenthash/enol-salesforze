package com.enol.salesforze.ui.visit

import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.ui.attendance.CameraCaptureView
import com.enol.salesforze.ui.attendance.CameraOverlayType
import com.enol.salesforze.ui.theme.BackgroundDark
import com.enol.salesforze.ui.theme.SurfaceDark
import com.enol.salesforze.ui.theme.SurfaceContainerDark
import com.enol.salesforze.ui.theme.OnSurfaceDark
import com.enol.salesforze.ui.theme.SlateSteelDark
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.ErrorCrimson
import com.enol.salesforze.ui.theme.LubricantAmber
import com.enol.salesforze.ui.theme.SlateSteel
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.SuccessEmerald
import com.enol.salesforze.ui.theme.WarningAmber
import com.enol.salesforze.ui.theme.elevation_2
import com.enol.salesforze.ui.theme.grid_1
import com.enol.salesforze.ui.theme.grid_2
import com.enol.salesforze.ui.theme.grid_3
import com.enol.salesforze.ui.theme.grid_4
import com.enol.salesforze.ui.theme.grid_6
import com.enol.salesforze.util.GeofenceUtil

/**
 * Geofenced Visit Check-In/Out & Outcome Execution Screen.
 * Enforces strict < 100m geofence lock, 2-minute anti-cheat minimum duration timer,
 * and comprehensive outcome tracking (Order, Payment Recovery, No-Order Reasons).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitExecutionScreen(
    modifier: Modifier = Modifier,
    viewModel: VisitViewModel = hiltViewModel(),
    onNavigateToOrderCart: (String) -> Unit = {},
    onVisitCompleted: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var cameraMode by remember { mutableStateOf<String?>(null) } // "SLIP" or "SHELF" or null
    var isForceCheckInExpanded by remember { mutableStateOf(false) }

    // Live Camera capture overlay (gallery pickers strictly disabled)
    if (cameraMode != null) {
        val title = if (cameraMode == "SLIP") "Payment Recovery Slip Capture" else "Store Shelf Merchandising Photo"
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_BACK,
            overlayType = CameraOverlayType.ODOMETER_RECTANGLE,
            title = title,
            instruction = "Frame document or display clearly in focus",
            onPhotoCaptured = { uri ->
                if (cameraMode == "SLIP") {
                    viewModel.onRecoverySlipPhotoCaptured(uri)
                } else {
                    viewModel.onShelfPhotoCaptured(uri)
                }
                cameraMode = null
            },
            onDismiss = { cameraMode = null }
        )
        return
    }

    if (uiState.isCompleted) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(grid_6)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(grid_6),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessEmerald,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(grid_3))
                        Text(
                            text = "Visit Completed & Synced",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EnolNavy
                        )
                        Spacer(modifier = Modifier.height(grid_2))
                        val durationMinutes = uiState.visitElapsedSeconds / 60
                        val durationSeconds = uiState.visitElapsedSeconds % 60
                        Text(
                            text = "Visit to ${uiState.selectedClient?.businessName} closed. Duration: %02d:%02d. Outcome: %s."
                                .format(durationMinutes, durationSeconds, uiState.selectedOutcome.replace("_", " ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(grid_6))
                        Button(
                            onClick = onVisitCompleted,
                            colors = ButtonDefaults.buttonColors(containerColor = EnolNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Return to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(grid_4)
        ) {
            // Error Message Banner
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = grid_3)
                ) {
                    Row(
                        modifier = Modifier.padding(grid_3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = ErrorCrimson)
                        Spacer(modifier = Modifier.width(grid_2))
                        Text(text = error, style = MaterialTheme.typography.bodySmall, color = ErrorCrimson)
                    }
                }
            }

            // 1. Client Card / Dropdown Picker
            uiState.selectedClient?.let { client ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(grid_3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = client.businessName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EnolNavy
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = client.approvalStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Proprietor: ${client.ownerName} • Phone: ${client.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteel
                        )
                        Text(
                            text = client.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // =================================================================
            // STEP 1: GEOFENCE VERIFICATION & CHECK-IN GATE
            // =================================================================
            if (uiState.activeVisit == null) {
                val distanceMeters = uiState.geofenceDistanceMeters ?: 9999.0
                val isInside = uiState.isWithinGeofence

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInside) SuccessEmerald.copy(alpha = 0.08f) else ErrorCrimson.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isInside) SuccessEmerald.copy(alpha = 0.5f) else ErrorCrimson.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(grid_3)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isInside) SuccessEmerald.copy(alpha = 0.2f) else ErrorCrimson.copy(alpha = 0.2f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isInside) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                        contentDescription = null,
                                        tint = if (isInside) SuccessEmerald else ErrorCrimson
                                    )
                                }
                                Spacer(modifier = Modifier.width(grid_3))
                                Column {
                                    Text(
                                        text = if (isInside) "On-Site Geofence Verified" else "Geofence Check-In Lock",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInside) SuccessEmerald else ErrorCrimson
                                    )
                                    Text(
                                        text = if (uiState.isGpsLocked)
                                            "${GeofenceUtil.formatDistance(distanceMeters)} from shop pin (Limit: 100m)"
                                        else
                                            "Acquiring high-accuracy satellite fix...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateSteel,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(onClick = viewModel::acquireGpsAndCheckGeofence) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh GPS")
                            }
                        }

                        // Force Check-In Option if Outside 100m Radius
                        if (!isInside && uiState.isGpsLocked) {
                            Spacer(modifier = Modifier.height(grid_3))
                            HorizontalDivider(color = ErrorCrimson.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(grid_2))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isForceCheckInExpanded = !isForceCheckInExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Force Check-In (Reason Required)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                Text(
                                    text = if (isForceCheckInExpanded) "Hide" else "Expand",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarningAmber
                                )
                            }

                            AnimatedVisibility(visible = isForceCheckInExpanded) {
                                Column(modifier = Modifier.padding(top = grid_2)) {
                                    OutlinedTextField(
                                        value = uiState.forceCheckInReason,
                                        onValueChange = viewModel::setForceCheckInReason,
                                        label = { Text("Deviation Reason (ASM Audit)") },
                                        placeholder = { Text("e.g. Shop relocated, GPS drift in covered market") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    Spacer(modifier = Modifier.height(grid_2))
                                    Button(
                                        onClick = { viewModel.performCheckIn(forceOverride = true) },
                                        enabled = uiState.forceCheckInReason.isNotBlank() && !uiState.isSubmitting,
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = EnolNavy),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "OVERRIDE & FORCE CHECK-IN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(grid_4))

                // Standard Check-In Button (Only enabled if inside 100m)
                Button(
                    onClick = { viewModel.performCheckIn(forceOverride = false) },
                    enabled = isInside && uiState.isGpsLocked && !uiState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LubricantAmber,
                        contentColor = EnolNavy
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(color = EnolNavy, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = "START VISIT CHECK-IN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else {
                // =============================================================
                // STEP 2: ACTIVE VISIT IN PROGRESS (ANTI-CHEAT STOPWATCH)
                // =============================================================

                // 1. 2-Minute Anti-Cheat Stopwatch Header
                val elapsedMinutes = uiState.visitElapsedSeconds / 60
                val elapsedSeconds = uiState.visitElapsedSeconds % 60
                val minSatisfied = uiState.isMinDurationSatisfied

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (minSatisfied) SuccessEmerald.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (minSatisfied) SuccessEmerald.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(grid_3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (minSatisfied) SuccessEmerald.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (minSatisfied) Icons.Default.CheckCircle else Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (minSatisfied) SuccessEmerald else WarningAmber
                                )
                            }
                            Spacer(modifier = Modifier.width(grid_3))
                            Column {
                                Text(
                                    text = if (minSatisfied) "Duration Satisfied" else "Mandatory Visit Timer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (minSatisfied)
                                        "Inspection elapsed: %02d:%02d".format(elapsedMinutes, elapsedSeconds)
                                    else
                                        "%02d:%02d remaining (Min 2:00 required)".format(
                                            uiState.remainingSecondsToMinVisit / 60,
                                            uiState.remainingSecondsToMinVisit % 60
                                        ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (minSatisfied) SuccessEmerald else WarningAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "%02d:%02d".format(elapsedMinutes, elapsedSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (minSatisfied) SuccessEmerald else WarningAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(grid_4))

                // 2. Outcome Logger Selection
                Text(text = "SELECT VISIT OUTCOME", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
                Spacer(modifier = Modifier.height(grid_1))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(grid_2)
                ) {
                    listOf(
                        "ORDER_BOOKED" to "Order",
                        "PAYMENT_RECOVERY" to "Recovery",
                        "NO_ORDER" to "No Order"
                    ).forEach { (outcomeKey, label) ->
                        val isSelected = uiState.selectedOutcome == outcomeKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EnolNavy else SlateSteel.copy(alpha = 0.1f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setOutcome(outcomeKey) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else SlateSteel,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = grid_2)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(grid_3))

                // Conditional Outcome Form
                when (uiState.selectedOutcome) {
                    "ORDER_BOOKED" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(grid_3)) {
                                Text(
                                    text = "Ready to Book Customer Order",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Launch the multi-SKU cart to add cartons, apply trade schemes, and verify credit limits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateSteel
                                )
                                Spacer(modifier = Modifier.height(grid_3))
                                Button(
                                    onClick = { onNavigateToOrderCart(uiState.selectedClientId ?: "") },
                                    colors = ButtonDefaults.buttonColors(containerColor = LubricantAmber, contentColor = EnolNavy),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(grid_2))
                                    Text(text = "LAUNCH ORDER CART", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "PAYMENT_RECOVERY" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(grid_3)) {
                                Text(text = "Record Payment Recovery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(grid_2))

                                OutlinedTextField(
                                    value = uiState.recoveryAmountText,
                                    onValueChange = { viewModel.setRecoveryFields(amount = it, slipNumber = uiState.recoverySlipNumber) },
                                    label = { Text("Recovery Amount (PKR) *") },
                                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = LubricantAmber) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(grid_2))

                                OutlinedTextField(
                                    value = uiState.recoverySlipNumber,
                                    onValueChange = { viewModel.setRecoveryFields(amount = uiState.recoveryAmountText, slipNumber = it) },
                                    label = { Text("Slip / Cheque / Tx Reference #") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(grid_2))

                                OutlinedButton(
                                    onClick = { cameraMode = "SLIP" },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(grid_2))
                                    Text(
                                        text = if (uiState.recoverySlipFile != null) "Slip Photo Attached (Retake)" else "Capture Slip Photo",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    "NO_ORDER" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(grid_3)) {
                                Text(text = "Specify No-Order Reason", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(grid_2))

                                var reasonExpanded by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = reasonExpanded,
                                    onExpandedChange = { reasonExpanded = !reasonExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = uiState.noOrderReason,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Primary Reason") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = reasonExpanded,
                                        onDismissRequest = { reasonExpanded = false }
                                    ) {
                                        uiState.noOrderReasons.forEach { reason ->
                                            DropdownMenuItem(
                                                text = { Text(reason) },
                                                onClick = {
                                                    viewModel.setNoOrderReason(reason)
                                                    reasonExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(grid_3))

                // 3. Merchandising / Shelf Photo Audit (Optional)
                OutlinedButton(
                    onClick = { cameraMode = "SHELF" },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(
                        text = if (uiState.shelfPhotoFile != null) "Shelf Audit Photo Attached (Retake)" else "Capture Shelf Merchandising Photo",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(grid_2))

                // 4. Notes
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::setNotes,
                    label = { Text("Visit Notes / Feedback") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(grid_6))

                // 5. Check-Out Button (Enforces 2-minute anti-cheat duration)
                val canCheckOut = minSatisfied && !uiState.isSubmitting

                Button(
                    onClick = { viewModel.performCheckOut() },
                    enabled = canCheckOut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (minSatisfied) SuccessEmerald else SlateSteel.copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else if (!minSatisfied) {
                        Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(grid_2))
                        Text(
                            text = "LOCKED: 02:00 MIN TIMER REQUIRED (%ds left)".format(uiState.remainingSecondsToMinVisit),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(grid_2))
                        Text(text = "CONCLUDE VISIT & CHECK OUT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
