package com.enol.salesforze.ui.attendance

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.enol.salesforze.ui.theme.EnolNavy
import com.enol.salesforze.ui.theme.LubricantAmber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visual framing overlays for camera capture guidance.
 */
enum class CameraOverlayType {
    /**
     * Oval framing mask optimized for sales representative facial selfie verification.
     */
    SELFIE_OVAL,

    /**
     * Rectangular wide-aspect framing mask optimized for vehicle odometer dash capture.
     */
    ODOMETER_RECTANGLE
}

/**
 * CameraX Live Capture Component.
 * Supports switching between front-facing camera (for selfie) and rear-facing camera (for odometer),
 * with custom viewfinder framing guides, flash control, and high-resolution JPEG persistence.
 */
@Composable
fun CameraCaptureView(
    modifier: Modifier = Modifier,
    initialLensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    overlayType: CameraOverlayType = CameraOverlayType.SELFIE_OVAL,
    title: String = "Align Subject in Frame",
    instruction: String = "Ensure clear lighting and sharp focus",
    onPhotoCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(initialLensFacing) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Rebind Camera whenever lens facing changes
    LaunchedEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            activeCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            // Reset torch state on camera switch
            isTorchEnabled = false
        } catch (e: Exception) {
            // Camera bind failure
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live Camera Preview Feed
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Translucent Cutout Framing Overlay
        ViewfinderOverlay(
            overlayType = overlayType,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top Control Bar (Close, Title, Flash Torch)
        Surface(
            color = EnolNavy.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = LubricantAmber,
                        fontSize = 11.sp
                    )
                }

                // Torch toggle (only applicable for rear camera)
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    IconButton(
                        onClick = {
                            activeCamera?.cameraControl?.let { control ->
                                isTorchEnabled = !isTorchEnabled
                                control.enableTorch(isTorchEnabled)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch Toggle",
                            tint = if (isTorchEnabled) LubricantAmber else Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }

        // 4. Bottom Controls (Lens Switch & Shutter Button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(vertical = 32.dp, horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Switch Lens (Front / Rear)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                            CameraSelector.LENS_FACING_BACK
                        } else {
                            CameraSelector.LENS_FACING_FRONT
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(4.dp, LubricantAmber, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) LubricantAmber.copy(alpha = 0.5f) else Color.White)
                        .clickable(enabled = !isCapturing) {
                            isCapturing = true
                            takePhoto(
                                context = context,
                                imageCapture = imageCapture,
                                onCaptured = { uri ->
                                    isCapturing = false
                                    onPhotoCaptured(uri)
                                },
                                onError = {
                                    isCapturing = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = EnolNavy,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Balance placeholder
                Spacer(modifier = Modifier.size(52.dp))
            }
        }
    }
}

/**
 * Custom Canvas drawing a darkened scrim with a transparent cutout matching the overlay guide.
 */
@Composable
private fun ViewfinderOverlay(
    overlayType: CameraOverlayType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val scrimColor = Color.Black.copy(alpha = 0.65f)
        val guideStrokeColor = Color.White.copy(alpha = 0.85f)
        val accentColor = Color(0xFFF5A623) // Lubricant Amber

        when (overlayType) {
            CameraOverlayType.SELFIE_OVAL -> {
                val ovalWidth = width * 0.72f
                val ovalHeight = height * 0.48f
                val left = (width - ovalWidth) / 2f
                val top = (height - ovalHeight) / 2.3f

                val ovalPath = Path().apply {
                    addOval(Rect(left, top, left + ovalWidth, top + ovalHeight))
                }

                clipPath(ovalPath, clipOp = ClipOp.Difference) {
                    drawRect(scrimColor)
                }

                // Draw decorative oval border
                drawOval(
                    color = accentColor,
                    topLeft = Offset(left, top),
                    size = Size(ovalWidth, ovalHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }
            CameraOverlayType.ODOMETER_RECTANGLE -> {
                val rectWidth = width * 0.85f
                val rectHeight = height * 0.28f
                val left = (width - rectWidth) / 2f
                val top = (height - rectHeight) / 2.2f
                val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())

                val rectPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(left, top, left + rectWidth, top + rectHeight),
                            radiusX = cornerRadius.x,
                            radiusY = cornerRadius.y
                        )
                    )
                }

                clipPath(rectPath, clipOp = ClipOp.Difference) {
                    drawRect(scrimColor)
                }

                // Draw decorative rectangular guide border
                drawRoundRect(
                    color = accentColor,
                    topLeft = Offset(left, top),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = cornerRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )

                // Draw central focus crosshair
                val centerX = width / 2f
                val centerY = top + (rectHeight / 2f)
                drawLine(
                    color = guideStrokeColor,
                    start = Offset(centerX - 24.dp.toPx(), centerY),
                    end = Offset(centerX + 24.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = guideStrokeColor,
                    start = Offset(centerX, centerY - 24.dp.toPx()),
                    end = Offset(centerX, centerY + 24.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/**
 * Captures image from CameraX [ImageCapture] usecase, writes to private app storage,
 * and passes the resulting [Uri] to the callback.
 */
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val outputDir = File(context.filesDir, "attendance_photos").apply {
        if (!exists()) mkdirs()
    }
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val photoFile = File(outputDir, "IMG_$timeStamp.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
