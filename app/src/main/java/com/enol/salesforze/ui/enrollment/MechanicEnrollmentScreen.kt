package com.enol.salesforze.ui.enrollment

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.ErrorCrimson
import com.enol.salesforze.ui.theme.LubricantAmber
import com.enol.salesforze.ui.theme.SlateSteel
import com.enol.salesforze.ui.theme.SuccessEmerald
import com.enol.salesforze.ui.theme.WarningAmber
import com.enol.salesforze.ui.theme.grid_1
import com.enol.salesforze.ui.theme.grid_2
import com.enol.salesforze.ui.theme.grid_3
import com.enol.salesforze.ui.theme.grid_4
import com.enol.salesforze.ui.theme.grid_6

/**
 * Mechanic Loyalty Program Onboarding Screen.
 * Captures mechanic identity, engine specialty, digital wallet info for QR cashback payouts,
 * and forces live GPS coordinate lock with on-site workshop camera capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicEnrollmentScreen(
    modifier: Modifier = Modifier,
    viewModel: EnrollmentViewModel = hiltViewModel(),
    onMechanicEnrolled: (String) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val state by viewModel.mechanicState.collectAsState()
    var isCameraActive by remember { mutableStateOf(false) }

    // Live Camera capture view (Gallery picker strictly omitted per anti-fraud rules)
    if (isCameraActive) {
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_BACK,
            overlayType = CameraOverlayType.ODOMETER_RECTANGLE,
            title = "Workshop Verification Capture",
            instruction = "Frame mechanic at workshop tool bench or storefront",
            onPhotoCaptured = { uri ->
                viewModel.onMechanicWorkshopPhotoCaptured(uri)
                isCameraActive = false
            },
            onDismiss = { isCameraActive = false }
        )
        return
    }

    if (state.submissionSuccess) {
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
                            text = "Mechanic Enrolled Successfully",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EnolNavy
                        )
                        Spacer(modifier = Modifier.height(grid_2))
                        Text(
                            text = "Ustad ${state.name} has been enrolled in the Enol Mechanic Loyalty Program with ${state.walletProvider} wallet payouts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(grid_6))
                        Button(
                            onClick = {
                                val mechanicId = state.createdMechanicId ?: ""
                                viewModel.resetMechanicForm()
                                onMechanicEnrolled(mechanicId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EnolNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Done", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mechanic Enrollment",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Enol Ustad Loyalty Program",
                            style = MaterialTheme.typography.bodySmall,
                            color = LubricantAmber
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EnolNavy)
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
            // Error Message Banner
            state.errorMessage?.let { error ->
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

            // 1. Mandatory GPS Lock Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isGpsLocked) SuccessEmerald.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isGpsLocked) SuccessEmerald.copy(alpha = 0.5f) else WarningAmber.copy(alpha = 0.4f)
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
                                    if (state.isGpsLocked) SuccessEmerald.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isGpsAcquiring) {
                                CircularProgressIndicator(color = WarningAmber, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(
                                    imageVector = if (state.isGpsLocked) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                    contentDescription = null,
                                    tint = if (state.isGpsLocked) SuccessEmerald else WarningAmber
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(grid_3))
                        Column {
                            Text(
                                text = if (state.isGpsLocked) "Workshop Location Fixed" else "Workshop GPS Required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isGpsLocked && state.gpsLatitude != null && state.gpsLongitude != null)
                                    "Accuracy ±${state.gpsAccuracyMeters?.toInt()}m • %.5f, %.5f".format(state.gpsLatitude, state.gpsLongitude)
                                else
                                    "Press to acquire on-site location",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isGpsLocked) SuccessEmerald else SlateSteel,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = viewModel::acquireMechanicGpsLock) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry GPS")
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 2. Personal & Workshop Details
            Text(text = "MECHANIC PROFILE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
            Spacer(modifier = Modifier.height(grid_1))

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onMechanicFieldChanged(name = it) },
                label = { Text("Mechanic Full Name (Ustad) *") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = LubricantAmber) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(grid_2))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(grid_2)) {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { viewModel.onMechanicFieldChanged(phone = it) },
                    label = { Text("Mobile Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = LubricantAmber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.cnic,
                    onValueChange = { viewModel.onMechanicFieldChanged(cnic = it) },
                    label = { Text("CNIC (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(grid_2))

            // Specialty Selector
            Text(text = "Engine / Vehicle Specialty", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = grid_1),
                horizontalArrangement = Arrangement.spacedBy(grid_1)
            ) {
                state.specialties.take(2).forEach { spec ->
                    val isSelected = state.specialty == spec
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EnolNavy else SlateSteel.copy(alpha = 0.1f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.onMechanicFieldChanged(specialty = spec) }
                    ) {
                        Text(
                            text = spec.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else SlateSteel,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = grid_2)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = grid_1),
                horizontalArrangement = Arrangement.spacedBy(grid_1)
            ) {
                state.specialties.drop(2).forEach { spec ->
                    val isSelected = state.specialty == spec
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EnolNavy else SlateSteel.copy(alpha = 0.1f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.onMechanicFieldChanged(specialty = spec) }
                    ) {
                        Text(
                            text = spec.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else SlateSteel,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = grid_2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 3. Digital Wallet Information for QR Payouts
            Text(text = "LOYALTY CASHBACK WALLET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
            Spacer(modifier = Modifier.height(grid_1))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(grid_2)) {
                // Wallet Provider Dropdown
                var walletExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = walletExpanded,
                    onExpandedChange = { walletExpanded = !walletExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.walletProvider,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = walletExpanded) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = walletExpanded,
                        onDismissRequest = { walletExpanded = false }
                    ) {
                        state.walletProviders.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider) },
                                onClick = {
                                    viewModel.onMechanicFieldChanged(walletProvider = provider)
                                    walletExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.walletAccountNumber,
                    onValueChange = { viewModel.onMechanicFieldChanged(walletAccountNumber = it) },
                    label = { Text("Account / Mobile # *") },
                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = LubricantAmber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 4. Linked Retail Workshop
            Text(text = "LINKED RETAIL SHOP (OPTIONAL)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
            Spacer(modifier = Modifier.height(grid_1))

            var clientExpanded by remember { mutableStateOf(false) }
            val linkedClientName = state.clients.find { it.id == state.selectedClientId }?.businessName ?: "Independent Workshop / Unlinked"
            ExposedDropdownMenuBox(
                expanded = clientExpanded,
                onExpandedChange = { clientExpanded = !clientExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = linkedClientName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Associated Lubricant Dealer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = clientExpanded,
                    onDismissRequest = { clientExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Independent Workshop (No linked dealer)") },
                        onClick = {
                            viewModel.onMechanicFieldChanged(clientId = "")
                            clientExpanded = false
                        }
                    )
                    state.clients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text("${client.businessName} (${client.ownerName})") },
                            onClick = {
                                viewModel.onMechanicFieldChanged(clientId = client.id)
                                clientExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 5. Workshop Photo Capture
            Text(text = "ON-SITE WORKSHOP PHOTO (LIVE CAMERA ONLY)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
            Spacer(modifier = Modifier.height(grid_1))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.watermarkedPhotoFile != null) SuccessEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.watermarkedPhotoFile != null) SuccessEmerald.copy(alpha = 0.5f) else SlateSteel.copy(alpha = 0.2f)
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
                                .size(42.dp)
                                .background(
                                    if (state.watermarkedPhotoFile != null) SuccessEmerald.copy(alpha = 0.2f) else EnolNavy.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isWatermarking) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = LubricantAmber)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = if (state.watermarkedPhotoFile != null) SuccessEmerald else EnolNavy
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(grid_3))
                        Column {
                            Text(
                                text = if (state.watermarkedPhotoFile != null) "Workshop Photo Captured" else "Live Camera Capture",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.watermarkedPhotoFile != null)
                                    "WebP compressed (<200KB) with forensic watermark"
                                else
                                    "Gallery picker disabled to prevent spoofing",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.watermarkedPhotoFile != null) SuccessEmerald else SlateSteel,
                                fontSize = 11.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { isCameraActive = true },
                        enabled = state.isGpsLocked,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = if (state.watermarkedPhotoFile != null) "Retake" else "Capture", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_6))

            // 6. Register Button
            val canSubmit = state.name.isNotBlank() &&
                    state.phone.isNotBlank() &&
                    state.walletAccountNumber.isNotBlank() &&
                    state.isGpsLocked &&
                    state.watermarkedPhotoFile != null &&
                    !state.isSubmitting

            Button(
                onClick = viewModel::submitMechanicEnrollment,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LubricantAmber,
                    contentColor = EnolNavy
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = EnolNavy, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(text = "Registering Mechanic...", fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "REGISTER MECHANIC & QUEUE SYNC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
