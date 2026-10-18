package com.lin0721.linmusic.core.friend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.lin0721.linmusic.core.log.AppLogger
import java.io.ByteArrayOutputStream

object ImageCompressor {
    private const val TAG = "ImageCompressor"

    fun compressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 80
    ): ByteArray? {
        return try {
            val resolver = context.contentResolver

            // 1. Decode bounds only to calculate inSampleSize
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            while (origWidth / (sampleSize * 2) >= maxDimension && origHeight / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            // 2. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Exact resize maintaining aspect ratio
            val maxSide = maxOf(sampledBitmap.width, sampledBitmap.height)
            val finalBitmap = if (maxSide > maxDimension) {
                val scale = maxDimension.toFloat() / maxSide
                val targetW = (sampledBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            } else {
                sampledBitmap
            }

            // 4. Compress to JPEG ByteArray
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()

            if (finalBitmap != sampledBitmap) {
                finalBitmap.recycle()
            }
            sampledBitmap.recycle()

            bytes
        } catch (e: Exception) {
            AppLogger.e(TAG, "Image compression failed for URI: $uri", e)
            null
        }
    }
}
