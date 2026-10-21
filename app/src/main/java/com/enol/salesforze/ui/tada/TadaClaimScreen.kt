package com.enol.salesforze.ui.tada

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.enol.salesforze.data.local.entities.TadaClaimEntity
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

/**
 * TADA Travel & Daily Allowance Expense Claim Screen.
 * Automatically reconciles physical odometer deltas with continuous GPS breadcrumb telemetry,
 * flagging claims with > 10% distance variance for managerial audit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TadaClaimScreen(
    modifier: Modifier = Modifier,
    viewModel: TadaViewModel = hiltViewModel(),
    onClaimSubmitted: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var cameraMode by remember { mutableStateOf<String?>(null) } // "ODOMETER" or "HOTEL" or null

    if (cameraMode != null) {
        val title = if (cameraMode == "ODOMETER") "Closing Odometer Photo" else "Hotel Bill / Night Stay Receipt"
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_BACK,
            overlayType = CameraOverlayType.ODOMETER_RECTANGLE,
            title = title,
            instruction = "Align readout / invoice clearly in good lighting",
            onPhotoCaptured = { uri ->
                if (cameraMode == "ODOMETER") {
                    viewModel.onClosingPhotoCaptured(uri)
                } else {
                    viewModel.onHotelBillPhotoCaptured(uri)
                }
                cameraMode = null
            },
            onDismiss = { cameraMode = null }
        )
        return
    }

    if (uiState.submissionSuccess) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(grid_6)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(grid_6),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (uiState.isDiscrepancyFlagged) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LubricantAmber,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(grid_3))
                        Text(
                            text = if (uiState.isDiscrepancyFlagged) "TADA Claim Flagged for Audit" else "TADA Claim Submitted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Spacer(modifier = Modifier.height(grid_2))
                        Text(
                            text = if (uiState.isDiscrepancyFlagged)
                                "Total Claim: PKR %,.0f. Distance variance (%.1f%%) exceeds 10%% threshold. Routed to ASM approval queue."
                                    .format(uiState.totalClaimAmount, uiState.variancePercentage)
                            else
                                "Total Claim: PKR %,.0f. GPS verification verified (%.1f%% variance). Queued for reimbursement."
                                    .format(uiState.totalClaimAmount, uiState.variancePercentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteelLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(grid_6))
                        Button(
                            onClick = {
                                viewModel.resetForm()
                                onClaimSubmitted()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LubricantAmber,
                                contentColor = BackgroundDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Return to Dashboard", color = BackgroundDark, fontWeight = FontWeight.Bold)
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

            // 1. Shift Telemetry Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(grid_4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shift Date: ${uiState.claimDate}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LubricantAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Rate: PKR ${uiState.fuelRatePerKm}/km",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LubricantAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_3))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Starting Odometer", style = MaterialTheme.typography.bodySmall, color = SlateSteelLight)
                            Text(
                                text = "${uiState.startOdometer.toInt()} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "GPS Trace Distance", style = MaterialTheme.typography.bodySmall, color = SlateSteelLight)
                            Text(
                                text = "%.1f km".format(uiState.gpsCalculatedKm),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LubricantAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 2. Closing Odometer Input & Photo
            Text(text = "CLOSING ODOMETER VERIFICATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteelLight)
            Spacer(modifier = Modifier.height(grid_1))

            OutlinedTextField(
                value = uiState.endOdometerText,
                onValueChange = viewModel::onEndOdometerChanged,
                label = { Text("Closing Odometer Reading (km) *") },
                placeholder = { Text("e.g. ${uiState.startOdometer.toInt() + 45}") },
                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = LubricantAmber) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LubricantAmber,
                    unfocusedBorderColor = SlateSteelDark,
                    focusedLabelColor = LubricantAmber,
                    unfocusedLabelColor = SlateSteelLight,
                    focusedTextColor = OnSurfaceDark,
                    unfocusedTextColor = OnSurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(grid_2))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.watermarkedOdometerFile != null) LubricantAmber.copy(alpha = 0.08f) else SurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.watermarkedOdometerFile != null) LubricantAmber.copy(alpha = 0.5f) else SlateSteelDark
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
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (uiState.watermarkedOdometerFile != null) LubricantAmber.copy(alpha = 0.2f) else SlateSteelDark,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isWatermarking) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LubricantAmber)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = if (uiState.watermarkedOdometerFile != null) LubricantAmber else SlateSteelLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(grid_3))
                        Column {
                            Text(
                                text = if (uiState.watermarkedOdometerFile != null) "Closing Odometer Attached" else "Rear Camera Odometer Photo *",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Text(
                                text = if (uiState.watermarkedOdometerFile != null) "Watermarked WebP (<200KB)" else "Mandatory visual mileage proof",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSteelLight,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { cameraMode = "ODOMETER" },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber)
                    ) {
                        Text(text = if (uiState.watermarkedOdometerFile != null) "Retake" else "Capture", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 3. DISCREPANCY ENGINE BANNER
            if (uiState.odometerDeltaKm > 0) {
                if (uiState.isDiscrepancyFlagged) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorCrimson.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(grid_3), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorCrimson, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(grid_3))
                            Column {
                                Text(
                                    text = "MILEAGE DISCREPANCY DETECTED",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ErrorCrimson
                                )
                                Text(
                                    text = "Variance: %.1f%% (> 10%% threshold). Odometer (+%.1f km) deviates from continuous GPS trace (%.1f km). Claim will be flagged for Manager audit."
                                        .format(uiState.variancePercentage, uiState.odometerDeltaKm, uiState.gpsCalculatedKm),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErrorCrimson,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LubricantAmber.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LubricantAmber.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(grid_3), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LubricantAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(grid_2))
                            Text(
                                text = "GPS Telemetry Verified • Variance: %.1f%% (Within 10%% allowable threshold)".format(uiState.variancePercentage),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = LubricantAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 4. Daily Allowance & Expense Details
            Text(text = "ALLOWANCE & EXPENSES BREAKDOWN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteelLight)
            Spacer(modifier = Modifier.height(grid_1))

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(grid_4)) {
                    // Fuel Allowance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = LubricantAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(grid_1))
                            Text(text = "Fuel Allowance (%.1f km)".format(uiState.odometerDeltaKm), style = MaterialTheme.typography.bodySmall, color = OnSurfaceDark)
                        }
                        Text(
                            text = "PKR %,.0f".format(uiState.fuelClaimAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                    }

                    Spacer(modifier = Modifier.height(grid_2))

                    // Outstation / Night Stay Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Hotel, contentDescription = null, tint = LubricantAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(grid_1))
                            Column {
                                Text(
                                    text = if (uiState.isOutstationNightStay) "Outstation / Night Stay DA" else "Local Field Shift DA",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceDark
                                )
                                Text(
                                    text = if (uiState.isOutstationNightStay) "PKR %,.0f rate (Hotel bill required)".format(uiState.outstationNightAllowance) else "PKR %,.0f local standard".format(uiState.localDailyAllowance),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateSteelLight,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isOutstationNightStay,
                            onCheckedChange = viewModel::toggleOutstationNightStay,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LubricantAmber,
                                checkedTrackColor = SurfaceContainerDark,
                                uncheckedThumbColor = SlateSteelLight,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }

                    // Hotel Bill Photo Upload if Outstation
                    AnimatedVisibility(visible = uiState.isOutstationNightStay) {
                        Column(modifier = Modifier.padding(top = grid_2)) {
                            OutlinedButton(
                                onClick = { cameraMode = "HOTEL" },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LubricantAmber),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(grid_1))
                                Text(
                                    text = if (uiState.watermarkedHotelBillFile != null) "Hotel Bill Attached (Retake)" else "Upload Hotel / Stay Bill *",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(grid_2))

                    // Toll / Parking Input
                    OutlinedTextField(
                        value = uiState.tollParkingText,
                        onValueChange = viewModel::onTollParkingChanged,
                        label = { Text("Toll Taxes & Parking (PKR)") },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = LubricantAmber) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LubricantAmber,
                            unfocusedBorderColor = SlateSteelDark,
                            focusedLabelColor = LubricantAmber,
                            unfocusedLabelColor = SlateSteelLight,
                            focusedTextColor = OnSurfaceDark,
                            unfocusedTextColor = OnSurfaceDark
                        )
                    )

                    Spacer(modifier = Modifier.height(grid_3))
                    HorizontalDivider(color = SlateSteelDark)
                    Spacer(modifier = Modifier.height(grid_2))

                    // Total Claim Sum
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL REIMBURSEMENT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "PKR %,.0f".format(uiState.totalClaimAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = LubricantAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_6))

            // 5. Submit Button
            val canSubmit = uiState.odometerDeltaKm > 0 &&
                    uiState.watermarkedOdometerFile != null &&
                    (!uiState.isOutstationNightStay || uiState.watermarkedHotelBillFile != null) &&
                    !uiState.isSubmitting

            Button(
                onClick = viewModel::submitTadaClaim,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LubricantAmber,
                    contentColor = BackgroundDark,
                    disabledContainerColor = SurfaceDark,
                    disabledContentColor = SlateSteelDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (uiState.isDiscrepancyFlagged) "SUBMIT CLAIM (WITH AUDIT FLAG)" else "SUBMIT TADA EXPENSE CLAIM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // 6. Recent Claims History
            if (uiState.userClaims.isNotEmpty()) {
                Spacer(modifier = Modifier.height(grid_6))
                Text(
                    text = "RECENT TADA SUBMISSIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SlateSteelLight
                )
                Spacer(modifier = Modifier.height(grid_2))

                uiState.userClaims.take(4).forEach { claim ->
                    TadaClaimHistoryCard(claim)
                    Spacer(modifier = Modifier.height(grid_2))
                }
            }
        }
    }
}

@Composable
private fun TadaClaimHistoryCard(claim: TadaClaimEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateSteelDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(grid_3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = claim.claimDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OnSurfaceDark)
                Text(
                    text = "${claim.odometerDeltaKm.toInt()} km  •  PKR %,.0f".format(claim.totalClaimAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateSteelLight,
                    fontSize = 11.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = LubricantAmber.copy(alpha = 0.15f)
            ) {
                Text(
                    text = claim.status,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = LubricantAmber,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
