package com.lin0721.linmusic.feature.player.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object MimikyuShareEngine {

    fun formatTimestamp(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    fun buildShareUrl(mediaId: String, timestampSec: Long?): String {
        val base = "https://music.163.com/song?id=$mediaId"
        return if (timestampSec != null && timestampSec > 0) {
            "$base&t=${timestampSec}s"
        } else {
            base
        }
    }

    fun copyLink(
        context: Context,
        mediaId: String,
        timestampSec: Long?,
        title: String,
        artist: String
    ) {
        val url = buildShareUrl(mediaId, timestampSec)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mimikyu Song Link", url)
        clipboard.setPrimaryClip(clip)
        val msg = if (timestampSec != null && timestampSec > 0) {
            "Link copied with timestamp (${formatTimestamp(timestampSec)})"
        } else {
            "Link copied to clipboard"
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    suspend fun renderShareCard(
        context: Context,
        title: String,
        artist: String,
        coverUrl: String,
        timestampSec: Long?,
        lyrics: String?,
        gradientStart: Int = 0xFF1E3264.toInt(),
        gradientEnd: Int = 0xFF121212.toInt()
    ): Pair<Bitmap, Uri?> = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw smooth vertical gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(gradientStart, gradientEnd),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Load cover image
        var coverBitmap: Bitmap? = null
        if (coverUrl.isNotBlank()) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    coverBitmap = result.drawable.toBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val density = context.resources.displayMetrics.density
        val isLyricsMode = !lyrics.isNullOrBlank()

        if (!isLyricsMode) {
            // === TRACK MODE ===
            val artSize = 720f
            val artLeft = (width - artSize) / 2f
            val artTop = 400f
            val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

            // Draw shadow for cover art
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x66000000
                setShadowLayer(40f, 0f, 25f, 0x88000000.toInt())
            }
            canvas.drawRoundRect(artRect, 32f, 32f, shadowPaint)

            // Draw cover art
            if (coverBitmap != null) {
                val scaledArt = Bitmap.createScaledBitmap(coverBitmap, artSize.toInt(), artSize.toInt(), true)
                val clipPath = Path().apply {
                    addRoundRect(artRect, 32f, 32f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(scaledArt, artLeft, artTop, Paint(Paint.ANTI_ALIAS_FLAG))
                canvas.restore()
            } else {
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF282828.toInt()
                }
                canvas.drawRoundRect(artRect, 32f, 32f, placeholderPaint)
            }

            // Timestamp Badge (if selected)
            var textStartY = artTop + artSize + 100f
            if (timestampSec != null && timestampSec > 0) {
                val tsText = "▶  ${formatTimestamp(timestampSec)}"
                val tsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1ED760.toInt() // Mimikyu Green
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val tsWidth = tsPaint.measureText(tsText)
                val badgePadH = 32f
                val badgePadV = 16f
                val badgeLeft = (width - tsWidth) / 2f - badgePadH
                val badgeTop = textStartY
                val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + tsWidth + badgePadH * 2, badgeTop + 60f)
                val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0x55000000
                }
                canvas.drawRoundRect(badgeRect, 30f, 30f, badgeBgPaint)
                canvas.drawText(tsText, badgeLeft + badgePadH, badgeTop + 42f, tsPaint)
                textStartY += 100f
            }

            // Song Title
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, width - 160)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(2)
                .build()
            canvas.save()
            canvas.translate(80f, textStartY)
            titleLayout.draw(canvas)
            canvas.restore()

            // Artist Name
            val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xB3FFFFFF.toInt() // 70% white
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val artistLayout = StaticLayout.Builder.obtain(artist, 0, artist.length, artistPaint, width - 160)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(1)
                .build()
            canvas.save()
            canvas.translate(80f, textStartY + titleLayout.height + 20f)
            artistLayout.draw(canvas)
            canvas.restore()

        } else {
            // === LYRICS QUOTE MODE ===
            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x88FFFFFF.toInt()
                textSize = 140f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            canvas.drawText("“", 90f, 400f, quotePaint)

            // Lyrics Text
            val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 62f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val lyricLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricPaint, width - 180)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(7)
                .setLineSpacing(24f, 1.15f)
                .build()
            canvas.save()
            canvas.translate(90f, 460f)
            lyricLayout.draw(canvas)
            canvas.restore()

            // Bottom Song Attribution
            val cardBottomY = height - 420f
            val miniArtSize = 130f
            val miniArtRect = RectF(90f, cardBottomY, 90f + miniArtSize, cardBottomY + miniArtSize)
            if (coverBitmap != null) {
                val scaledArt = Bitmap.createScaledBitmap(coverBitmap, miniArtSize.toInt(), miniArtSize.toInt(), true)
                val clipPath = Path().apply { addRoundRect(miniArtRect, 18f, 18f, Path.Direction.CW) }
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(scaledArt, 90f, cardBottomY, Paint(Paint.ANTI_ALIAS_FLAG))
                canvas.restore()
            }

            val miniTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(title, 250f, cardBottomY + 54f, miniTitlePaint)

            val miniArtistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xB3FFFFFF.toInt()
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText(artist, 250f, cardBottomY + 110f, miniArtistPaint)
        }

        // 3. Mimikyu Soundwave Code & Branding at Bottom
        drawSoundwaveBranding(canvas, width, height)

        // 4. Save to temporary file in cacheDir/share/
        var fileUri: Uri? = null
        try {
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val shareFile = File(shareDir, "mimikyu_share_${System.currentTimeMillis()}.png")
            FileOutputStream(shareFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        Pair(bitmap, fileUri)
    }

    private fun drawSoundwaveBranding(canvas: Canvas, width: Int, height: Int) {
        val baseY = height - 160f
        val waveBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xDDFFFFFF.toInt()
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 9f
        }

        // Draw rhythmic soundwave code bars
        val barHeights = floatArrayOf(
            16f, 32f, 22f, 48f, 36f, 64f, 28f, 52f, 74f, 40f,
            24f, 56f, 70f, 32f, 44f, 60f, 28f, 68f, 50f, 22f,
            46f, 62f, 34f, 20f, 54f, 42f, 26f, 38f, 18f
        )
        val spacing = 22f
        val totalWaveWidth = barHeights.size * spacing
        val startX = (width - totalWaveWidth) / 2f

        for (i in barHeights.indices) {
            val x = startX + i * spacing
            val h = barHeights[i]
            canvas.drawLine(x, baseY - h / 2f, x, baseY + h / 2f, waveBarPaint)
        }

        // Branding Label "MIMIKYU"
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            textSize = 28f
            letterSpacing = 0.28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val brandText = "OCTOBER"
        val brandWidth = brandPaint.measureText(brandText)
        canvas.drawText(brandText, (width - brandWidth) / 2f, height - 70f, brandPaint)
    }

    fun shareToInstagramStories(context: Context, imageUri: Uri?) {
        if (imageUri == null) {
            Toast.makeText(context, "Failed to create share card", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(imageUri, "image/png")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra("interactive_asset_uri", imageUri)
            putExtra("content_url", "https://october.app")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to system chooser with image
            shareSystem(context, "Listen on October", imageUri)
        }
    }

    fun shareToWhatsApp(context: Context, text: String, imageUri: Uri?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (imageUri != null) "image/png" else "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, text)
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareSystem(context, text, imageUri)
        }
    }

    fun shareToSnapchat(context: Context, imageUri: Uri?) {
        if (imageUri == null) {
            Toast.makeText(context, "Failed to create share card", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            `package` = "com.snapchat.android"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareSystem(context, "Listen on October", imageUri)
        }
    }

    fun shareToTwitter(context: Context, text: String, url: String) {
        val fullText = "$text $url"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.twitter.android"
            putExtra(Intent.EXTRA_TEXT, fullText)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser tweet intent
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/intent/tweet?text=${Uri.encode(fullText)}"))
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                shareSystem(context, fullText, null)
            }
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap) {
        try {
            val filename = "October_Share_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/October")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/October"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                Toast.makeText(context, "Share card saved to gallery", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save card: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareSystem(context: Context, text: String, imageUri: Uri?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
