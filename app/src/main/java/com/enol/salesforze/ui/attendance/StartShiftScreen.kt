package com.enol.salesforze.ui.attendance

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.enol.salesforze.ui.theme.BackgroundDark
import com.enol.salesforze.ui.theme.SurfaceDark
import com.enol.salesforze.ui.theme.SurfaceContainerDark
import com.enol.salesforze.ui.theme.OnSurfaceDark
import com.enol.salesforze.ui.theme.SlateSteelDark
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.BackgroundDark
import com.enol.salesforze.ui.theme.SurfaceDark
import com.enol.salesforze.ui.theme.SurfaceContainerDark
import com.enol.salesforze.ui.theme.OnSurfaceDark
import com.enol.salesforze.ui.theme.SlateSteelDark
import com.enol.salesforze.ui.theme.SlateSteelLight
import com.enol.salesforze.ui.theme.EnolNavyLight
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
 * Start Field Day Screen.
 * Enforces the 3-step mandatory verification workflow:
 * 1. Live front-camera selfie.
 * 2. Live rear-camera vehicle odometer capture.
 * 3. High-accuracy GPS lock & anti-spoofing verification before shift ignition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartShiftScreen(
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel(),
    onShiftStarted: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Navigation trigger when shift becomes active
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep == ShiftStep.ACTIVE_SHIFT) {
            onShiftStarted()
        }
    }

    // Permission handling for CAMERA and LOCATION
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        if (hasLocationPermission && !uiState.isGpsLocked) {
            viewModel.acquireGpsLock()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else if (!uiState.isGpsLocked) {
            viewModel.acquireGpsLock()
        }
    }

    // Fullscreen Camera View Overlay for Selfie Capture
    if (uiState.currentStep == ShiftStep.CAPTURE_SELFIE) {
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_FRONT,
            overlayType = CameraOverlayType.SELFIE_OVAL,
            title = "Sales Rep Verification",
            instruction = "Frame your face clearly in good lighting",
            onPhotoCaptured = { uri -> viewModel.onSelfieCaptured(uri) },
            onDismiss = { viewModel.navigateToStep(ShiftStep.START_DAY_INITIAL) }
        )
        return
    }

    // Fullscreen Camera View Overlay for Vehicle Odometer Capture
    if (uiState.currentStep == ShiftStep.CAPTURE_ODOMETER) {
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_BACK,
            overlayType = CameraOverlayType.ODOMETER_RECTANGLE,
            title = "Vehicle Odometer Capture",
            instruction = "Align dashboard odometer readout inside the rectangle",
            onPhotoCaptured = { uri -> viewModel.onOdometerPhotoCaptured(uri) },
            onDismiss = { viewModel.navigateToStep(ShiftStep.START_DAY_INITIAL) }
        )
        return
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
            // 1. Anti-Spoofing & Security Alert Banner (if flagged)
            if (uiState.isMockBlocked) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorCrimson.copy(alpha = 0.15f)),
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = ErrorCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(grid_3))
                        Text(
                            text = "Fake GPS / Mock Location detected! Field actions are blocked until simulated locations are disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorCrimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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

            // 2. Step Progress Indicator
            StepProgressHeader(
                selfieDone = uiState.selfieUri != null,
                odometerDone = uiState.odometerPhotoUri != null,
                gpsDone = uiState.isGpsLocked
            )

            Spacer(modifier = Modifier.height(grid_4))

            // 3. Step 1: Selfie Facial Verification Card
            VerificationActionCard(
                title = "1. Facial Selfie Verification",
                subtitle = if (uiState.selfieUri != null) "Face verified & photo attached" else "Front camera photo required",
                isCompleted = uiState.selfieUri != null,
                icon = Icons.Default.Face,
                buttonLabel = if (uiState.selfieUri != null) "Retake Selfie" else "Capture Selfie",
                onClick = { viewModel.navigateToStep(ShiftStep.CAPTURE_SELFIE) }
            )

            Spacer(modifier = Modifier.height(grid_3))

            // 4. Step 2: Vehicle Odometer & Mileage Card
            VerificationActionCard(
                title = "2. Vehicle Odometer Photo",
                subtitle = if (uiState.odometerPhotoUri != null) "Odometer dash photo attached" else "Rear camera photo required",
                isCompleted = uiState.odometerPhotoUri != null,
                icon = Icons.Default.DirectionsCar,
                buttonLabel = if (uiState.odometerPhotoUri != null) "Retake Photo" else "Capture Odometer",
                onClick = { viewModel.navigateToStep(ShiftStep.CAPTURE_ODOMETER) }
            )

            Spacer(modifier = Modifier.height(grid_3))

            // 5. Starting Odometer Input Field
            OutlinedTextField(
                value = uiState.odometerReading,
                onValueChange = viewModel::onOdometerReadingChanged,
                label = { Text("Starting Odometer Reading (km)") },
                placeholder = { Text("e.g. 42350") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speedometer",
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

            Spacer(modifier = Modifier.height(grid_3))

            // 6. Step 3: High-Accuracy GPS Lock Status Card
            GpsStatusCard(
                isLocked = uiState.isGpsLocked,
                isAcquiring = uiState.isGpsAcquiring,
                latitude = uiState.gpsLatitude,
                longitude = uiState.gpsLongitude,
                accuracyMeters = uiState.gpsAccuracyMeters,
                onRetryGps = viewModel::acquireGpsLock
            )

            Spacer(modifier = Modifier.height(grid_6))

            // 7. Submit Shift Start Button
            val canSubmit = uiState.selfieUri != null &&
                    uiState.odometerPhotoUri != null &&
                    uiState.odometerReading.isNotBlank() &&
                    uiState.isGpsLocked &&
                    !uiState.isMockBlocked &&
                    !uiState.isSubmitting

            Button(
                onClick = viewModel::submitStartShift,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LubricantAmber,
                    contentColor = BackgroundDark,
                    disabledContainerColor = SlateSteelDark,
                    disabledContentColor = SlateSteelLight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        color = BackgroundDark,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(
                        text = "Igniting Field Day...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BackgroundDark
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Start",
                        modifier = Modifier.size(20.dp),
                        tint = BackgroundDark
                    )
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(
                        text = "START FIELD SHIFT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BackgroundDark
                    )
                }
            }
        }
    }
}

/**
 * Visual 3-step progress bar with checkmark circles.
 */
@Composable
private fun StepProgressHeader(
    selfieDone: Boolean,
    odometerDone: Boolean,
    gpsDone: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation_2),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = grid_3, horizontal = grid_4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepPill(label = "1. Selfie", isCompleted = selfieDone)
            Text(text = "•", color = SlateSteelDark, fontWeight = FontWeight.Bold)
            StepPill(label = "2. Odometer", isCompleted = odometerDone)
            Text(text = "•", color = SlateSteelDark, fontWeight = FontWeight.Bold)
            StepPill(label = "3. GPS Fix", isCompleted = gpsDone)
        }
    }
}

@Composable
private fun StepPill(
    label: String,
    isCompleted: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (isCompleted) LubricantAmber else SlateSteelDark,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BackgroundDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(grid_1))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isCompleted) LubricantAmber else SlateSteelLight
        )
    }
}

@Composable
private fun VerificationActionCard(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) LubricantAmber.copy(alpha = 0.08f) else SurfaceDark
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCompleted) LubricantAmber.copy(alpha = 0.5f) else SlateSteelDark
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
                            if (isCompleted) LubricantAmber.copy(alpha = 0.2f) else SlateSteelDark,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isCompleted) LubricantAmber else SlateSteelLight,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(grid_3))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) LubricantAmber else SlateSteelLight,
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isCompleted) LubricantAmber else SlateSteelLight
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun GpsStatusCard(
    isLocked: Boolean,
    isAcquiring: Boolean,
    latitude: Double?,
    longitude: Double?,
    accuracyMeters: Float?,
    onRetryGps: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) LubricantAmber.copy(alpha = 0.08f) else SurfaceDark
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isLocked) LubricantAmber.copy(alpha = 0.5f) else SlateSteelDark
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
                            if (isLocked) LubricantAmber.copy(alpha = 0.2f) else SlateSteelDark,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAcquiring) {
                        CircularProgressIndicator(
                            color = LubricantAmber,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                            contentDescription = "GPS Status",
                            tint = if (isLocked) LubricantAmber else SlateSteelLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(grid_3))
                Column {
                    Text(
                        text = if (isLocked) "GPS Lock Acquired" else if (isAcquiring) "Triangulating GPS..." else "GPS Lock Required",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceDark
                    )
                    Text(
                        text = if (isLocked && latitude != null && longitude != null) {
                            "Accuracy: ±${accuracyMeters?.toInt() ?: 0}m • Lat: %.4f, Lng: %.4f".format(latitude, longitude)
                        } else if (isAcquiring) {
                            "Locking satellites (threshold <= 25m)..."
                        } else {
                            "Press to acquire high-accuracy satellite fix"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLocked) LubricantAmber else SlateSteelLight,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onRetryGps,
                enabled = !isAcquiring
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh GPS",
                    tint = if (isLocked) LubricantAmber else SlateSteelLight
                )
            }
        }
    }
}
