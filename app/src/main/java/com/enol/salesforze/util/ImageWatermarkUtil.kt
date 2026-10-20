package com.enol.salesforze.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Image processing engine that burns forensic watermarks into field photos
 * and compresses them to high-density WebP (< 200KB) for bandwidth-efficient sync.
 */
object ImageWatermarkUtil {

    private const val MAX_IMAGE_DIMENSION = 1280 // Max width/height to constrain memory
    private const val TARGET_MAX_FILE_SIZE_BYTES = 200 * 1024 // 200 KB target

    /**
     * Burns metadata (rep ID, timestamp, coordinates) directly into the image canvas
     * and compresses the output to a WebP file.
     *
     * @param context Android context
     * @param sourceUri Original photo URI
     * @param salesRepId Authenticated sales rep UUID/Identifier
     * @param latitude Current verified GPS latitude
     * @param longitude Current verified GPS longitude
     * @param accuracyMeters GPS accuracy radius
     * @param subDirectory Sub-folder in filesDir (e.g. "enrollments", "visits")
     * @return Resulting watermarked WebP [File]
     */
    suspend fun watermarkAndCompressImage(
        context: Context,
        sourceUri: Uri,
        salesRepId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float? = null,
        subDirectory: String = "watermarked_photos"
    ): File = withContext(Dispatchers.IO) {
        // 1. Decode bitmap with inSampleSize subsampling
        val originalBitmap = decodeSampledBitmapFromUri(context, sourceUri, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            ?: throw IllegalArgumentException("Unable to decode image from $sourceUri")

        // 2. Create mutable working bitmap
        val workingBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(workingBitmap)
        val width = workingBitmap.width.toFloat()
        val height = workingBitmap.height.toFloat()

        // 3. Draw forensic dark banner at the bottom
        val bannerHeight = (height * 0.14f).coerceAtLeast(100f)
        val bannerPaint = Paint().apply {
            color = Color.argb(190, 10, 25, 47) // Enol Navy with alpha
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, height - bannerHeight, width, height, bannerPaint)

        // Draw top accent line on the banner
        val accentPaint = Paint().apply {
            color = Color.rgb(245, 166, 35) // Lubricant Amber
            strokeWidth = (bannerHeight * 0.04f).coerceAtLeast(4f)
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, height - bannerHeight, width, height - bannerHeight, accentPaint)

        // 4. Draw Watermark Typography
        val titleTextSize = (bannerHeight * 0.26f).coerceIn(24f, 44f)
        val bodyTextSize = (bannerHeight * 0.20f).coerceIn(18f, 32f)

        val titlePaint = Paint().apply {
            color = Color.rgb(245, 166, 35) // Lubricant Amber
            textSize = titleTextSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = bodyTextSize
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val paddingLeft = width * 0.03f
        val line1Y = height - bannerHeight + (bannerHeight * 0.32f)
        val line2Y = height - bannerHeight + (bannerHeight * 0.62f)
        val line3Y = height - bannerHeight + (bannerHeight * 0.88f)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date())
        val accuracyStr = if (accuracyMeters != null) " (±${accuracyMeters.toInt()}m)" else ""

        canvas.drawText("ENOL SALESFORZE • ON-SITE VERIFIED", paddingLeft, line1Y, titlePaint)
        canvas.drawText("REP: $salesRepId  |  $timestamp", paddingLeft, line2Y, textPaint)
        canvas.drawText("GPS: %.5f, %.5f%s".format(latitude, longitude, accuracyStr), paddingLeft, line3Y, textPaint)

        // 5. Compress to WebP targeting < 200KB
        val outputDir = File(context.filesDir, subDirectory).apply {
            if (!exists()) mkdirs()
        }
        val outputFile = File(outputDir, "ENOL_${UUID.randomUUID()}.webp")

        var quality = 85
        var compressedBytes: ByteArray
        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        do {
            val byteStream = ByteArrayOutputStream()
            workingBitmap.compress(compressFormat, quality, byteStream)
            compressedBytes = byteStream.toByteArray()
            quality -= 10
        } while (compressedBytes.size > TARGET_MAX_FILE_SIZE_BYTES && quality >= 45)

        FileOutputStream(outputFile).use { it.write(compressedBytes) }

        // Recycle bitmaps
        originalBitmap.recycle()
        workingBitmap.recycle()

        outputFile
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val resolver = context.contentResolver

        // 1. Decode bounds only
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        // 2. Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        // 3. Decode scaled bitmap
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
