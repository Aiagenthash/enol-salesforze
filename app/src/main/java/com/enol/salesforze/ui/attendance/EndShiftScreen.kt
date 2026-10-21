package com.enol.salesforze.ui.attendance

import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.ErrorCrimson
import com.enol.salesforze.ui.theme.LubricantAmber
import com.enol.salesforze.ui.theme.SlateSteel
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.SuccessEmerald
import com.enol.salesforze.ui.theme.elevation_2
import com.enol.salesforze.ui.theme.grid_1
import com.enol.salesforze.ui.theme.grid_2
import com.enol.salesforze.ui.theme.grid_3
import com.enol.salesforze.ui.theme.grid_4
import com.enol.salesforze.ui.theme.grid_6

/**
 * End Field Day Screen.
 * Captures closing odometer photo and mileage, validates positive distance delta,
 * stops foreground location tracking, and generates daily field summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndShiftScreen(
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel(),
    onShiftEnded: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var isCapturingCamera by remember { mutableStateOf(false) }

    // Fullscreen Camera View Overlay for Closing Odometer Capture
    if (isCapturingCamera) {
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_BACK,
            overlayType = CameraOverlayType.ODOMETER_RECTANGLE,
            title = "Closing Odometer Capture",
            instruction = "Frame vehicle odometer readout clearly",
            onPhotoCaptured = { uri ->
                viewModel.onEndOdometerPhotoCaptured(uri)
                isCapturingCamera = false
            },
            onDismiss = { isCapturingCamera = false }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.currentStep == ShiftStep.SHIFT_SUMMARY) "Shift Summary" else "End Field Day",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Enol Shift Reconciliation",
                            style = MaterialTheme.typography.bodySmall,
                            color = LubricantAmber
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null && uiState.currentStep != ShiftStep.SHIFT_SUMMARY) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EnolNavy
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(grid_4)
        ) {
            // Render Shift Summary Card if shift is ended
            if (uiState.currentStep == ShiftStep.SHIFT_SUMMARY) {
                ShiftCompletedSummaryCard(
                    totalHours = uiState.shiftDurationHours ?: 0.0,
                    totalKm = uiState.totalKmDriven ?: 0.0,
                    onDone = {
                        viewModel.resetToStart()
                        onShiftEnded()
                    }
                )
                return@Column
            }

            // Error Message Banner
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = grid_3)
                ) {
                    Row(
                        modifier = Modifier.padding(grid_3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = ErrorCrimson
                        )
                        Spacer(modifier = Modifier.width(grid_2))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorCrimson,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 1. Shift In-Progress Header Card
            val activeShift = uiState.activeShift
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(grid_4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(SuccessEmerald, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(grid_2))
                            Text(
                                text = "ACTIVE FIELD DAY",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessEmerald,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = activeShift?.date ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteel
                        )
                    }

                    Spacer(modifier = Modifier.height(grid_3))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Shift Start Time",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSteel
                            )
                            Text(
                                text = activeShift?.shiftStart?.takeLast(13)?.take(8) ?: "08:30:00",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Starting Odometer",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateSteel
                            )
                            Text(
                                text = "${activeShift?.startOdometerReading?.toInt() ?: 0} km",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LubricantAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 2. Closing Odometer Photo Capture Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.endOdometerPhotoUri != null) SuccessEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.endOdometerPhotoUri != null) SuccessEmerald.copy(alpha = 0.5f) else SlateSteel.copy(alpha = 0.2f)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (uiState.endOdometerPhotoUri != null) SuccessEmerald.copy(alpha = 0.2f) else EnolNavy.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (uiState.endOdometerPhotoUri != null) SuccessEmerald else EnolNavy,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(grid_3))
                        Column {
                            Text(
                                text = "Closing Odometer Photo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.endOdometerPhotoUri != null) "Closing photo attached" else "Rear camera photo required",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.endOdometerPhotoUri != null) SuccessEmerald else SlateSteel,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { isCapturingCamera = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (uiState.endOdometerPhotoUri != null) SuccessEmerald else EnolNavy
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (uiState.endOdometerPhotoUri != null) "Retake" else "Capture",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_3))

            // 3. Closing Odometer Reading Input
            OutlinedTextField(
                value = uiState.endOdometerReading,
                onValueChange = viewModel::onEndOdometerReadingChanged,
                label = { Text("Closing Odometer Reading (km)") },
                placeholder = { Text("Must be >= ${activeShift?.startOdometerReading?.toInt() ?: 0} km") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Odometer",
                        tint = LubricantAmber
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LubricantAmber,
                    unfocusedBorderColor = SlateSteel.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Live Delta Distance Preview
            val startKm = activeShift?.startOdometerReading ?: 0.0
            val endKm = uiState.endOdometerReading.toDoubleOrNull()
            if (endKm != null && endKm >= startKm) {
                val deltaKm = Math.round((endKm - startKm) * 10.0) / 10.0
                Spacer(modifier = Modifier.height(grid_2))
                Text(
                    text = "Total Day Distance: +$deltaKm km traveled",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessEmerald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = grid_2)
                )
            }

            Spacer(modifier = Modifier.height(grid_6))

            // 4. End Shift Button
            val canClose = uiState.endOdometerPhotoUri != null &&
                    uiState.endOdometerReading.isNotBlank() &&
                    (endKm ?: 0.0) >= startKm &&
                    !uiState.isSubmitting

            Button(
                onClick = viewModel::submitEndShift,
                enabled = canClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorCrimson,
                    contentColor = Color.White,
                    disabledContainerColor = SlateSteel.copy(alpha = 0.2f),
                    disabledContentColor = SlateSteelLight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(
                        text = "Reconciling Shift...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "End Shift",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(
                        text = "CONCLUDE FIELD DAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

/**
 * Summary card displayed after shift conclusion.
 */
@Composable
private fun ShiftCompletedSummaryCard(
    totalHours: Double,
    totalKm: Double,
    onDone: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(grid_6),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SuccessEmerald.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = SuccessEmerald,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(grid_3))

            Text(
                text = "Shift Concluded Successfully",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = EnolNavy
            )
            Text(
                text = "GPS tracking stopped. Attendance record and routes queued for sync.",
                style = MaterialTheme.typography.bodySmall,
                color = SlateSteel,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = grid_1)
            )

            Spacer(modifier = Modifier.height(grid_4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Total Hours", style = MaterialTheme.typography.bodySmall, color = SlateSteel)
                    Text(
                        text = "%.2f hrs".format(totalHours),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EnolNavy
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(SlateSteel.copy(alpha = 0.2f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Distance Driven", style = MaterialTheme.typography.bodySmall, color = SlateSteel)
                    Text(
                        text = "%.1f km".format(totalKm),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LubricantAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(grid_6))

            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnolNavy,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(grid_2))
                Text(text = "Return to Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}
