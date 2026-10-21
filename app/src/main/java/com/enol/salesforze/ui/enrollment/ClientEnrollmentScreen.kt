package com.enol.salesforze.ui.enrollment

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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LocationOn
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

/**
 * On-Site Customer & Retailer Onboarding Screen.
 * Enforces live GPS coordinate lock (<= 25m) and live camera capture with WebP forensic watermarking.
 * Gallery pickers are strictly disabled to prevent spoofed/unverified dealer enrollments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEnrollmentScreen(
    modifier: Modifier = Modifier,
    viewModel: EnrollmentViewModel = hiltViewModel(),
    onClientEnrolled: (String) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val state by viewModel.clientState.collectAsState()
    var isCameraActive by remember { mutableStateOf(false) }

    // Live Camera capture view (Gallery picker strictly omitted per enterprise policy)
    if (isCameraActive) {
        CameraCaptureView(
            initialLensFacing = CameraSelector.LENS_FACING_BACK,
            overlayType = CameraOverlayType.ODOMETER_RECTANGLE,
            title = "Storefront On-Site Capture",
            instruction = "Frame the main shop signboard and entrance clearly",
            onPhotoCaptured = { uri ->
                viewModel.onClientStorefrontPhotoCaptured(uri)
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
                            text = "Retailer Enrolled Successfully",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EnolNavy
                        )
                        Spacer(modifier = Modifier.height(grid_2))
                        Text(
                            text = "${state.businessName} has been recorded locally with verified GPS coordinates and queued for manager approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateSteel,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(grid_6))
                        Button(
                            onClick = {
                                val clientId = state.createdClientId ?: ""
                                viewModel.resetClientForm()
                                onClientEnrolled(clientId)
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

            // 1. GPS Lock Card
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
                                text = if (state.isGpsLocked) "On-Site GPS Pin Locked" else "Mandatory GPS Fix Required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isGpsLocked && state.gpsLatitude != null && state.gpsLongitude != null)
                                    "Accuracy ±${state.gpsAccuracyMeters?.toInt()}m • %.5f, %.5f".format(state.gpsLatitude, state.gpsLongitude)
                                else
                                    "Press to triangulate store location",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isGpsLocked) SuccessEmerald else SlateSteel,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = viewModel::acquireClientGpsLock) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry GPS")
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_4))

            // 2. Business Details Fields
            Text(text = "BUSINESS DETAILS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
            Spacer(modifier = Modifier.height(grid_1))

            OutlinedTextField(
                value = state.businessName,
                onValueChange = { viewModel.onClientFieldChanged(businessName = it) },
                label = { Text("Shop / Business Name *") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = LubricantAmber) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(grid_2))

            OutlinedTextField(
                value = state.ownerName,
                onValueChange = { viewModel.onClientFieldChanged(ownerName = it) },
                label = { Text("Owner / Proprietor Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(grid_2))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(grid_2)) {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { viewModel.onClientFieldChanged(phone = it) },
                    label = { Text("Phone Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = LubricantAmber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = state.cnic,
                    onValueChange = { viewModel.onClientFieldChanged(cnic = it) },
                    label = { Text("CNIC (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(grid_2))

            OutlinedTextField(
                value = state.address,
                onValueChange = { viewModel.onClientFieldChanged(address = it) },
                label = { Text("Complete Shop Address *") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = LubricantAmber) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(grid_4))

            // 3. District & Credit Limit
            Text(text = "TERRITORY & CREDIT ASSIGNMENT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
            Spacer(modifier = Modifier.height(grid_1))

            // District Dropdown
            var districtExpanded by remember { mutableStateOf(false) }
            val selectedDistrictName = state.districts.find { it.id == state.selectedDistrictId }?.name ?: "Select District"
            ExposedDropdownMenuBox(
                expanded = districtExpanded,
                onExpandedChange = { districtExpanded = !districtExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDistrictName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("District Assignment *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = districtExpanded,
                    onDismissRequest = { districtExpanded = false }
                ) {
                    state.districts.forEach { district ->
                        DropdownMenuItem(
                            text = { Text("${district.name} (${district.province})") },
                            onClick = {
                                viewModel.onClientFieldChanged(districtId = district.id)
                                districtExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(grid_2))

            OutlinedTextField(
                value = state.creditLimitText,
                onValueChange = { viewModel.onClientFieldChanged(creditLimit = it) },
                label = { Text("Requested Credit Limit (PKR)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(grid_4))

            // 4. Storefront Camera Capture (WebP Forensics)
            Text(text = "VERIFIED STOREFRONT PHOTO (LIVE CAMERA ONLY)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SlateSteel)
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
                                text = if (state.watermarkedPhotoFile != null) "Storefront Photo Captured" else "Live Camera Capture",
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

            // 5. Submit Button
            val canSubmit = state.businessName.isNotBlank() &&
                    state.ownerName.isNotBlank() &&
                    state.phone.isNotBlank() &&
                    state.isGpsLocked &&
                    state.watermarkedPhotoFile != null &&
                    !state.isSubmitting

            Button(
                onClick = viewModel::submitClientEnrollment,
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LubricantAmber,
                    contentColor = BackgroundDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(grid_2))
                    Text(text = "Enrolling Retailer...", fontWeight = FontWeight.Bold, color = BackgroundDark)
                } else {
                    Text(text = "ENROLL CUSTOMER & QUEUE SYNC", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BackgroundDark)
                }
            }
        }
    }
}
