package com.lin0721.linmusic.core.update.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed class DownloadState {
    data class Downloading(val progress: Int) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

class ApkDownloader(
    private val context: Context,
    private val downloadClient: OkHttpClient
) {

    // of ，Download
    fun download(url: String, versionName: String): Flow<DownloadState> = flow {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val targetFile = File(dir, "Mimikyu-$versionName.apk")
        val tempFile = File(dir, "Mimikyu-$versionName.apk.tmp")

        val request = Request.Builder().url(url).build()
        val call = downloadClient.newCall(request)

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failed("Download failed: HTTP ${response.code}"))
                    return@flow
                }
                val body = response.body
                if (body == null) {
                    emit(DownloadState.Failed("Download failed: empty response"))
                    return@flow
                }

                val total = body.contentLength()
                val isTargetValid = targetFile.exists() &&
                        (total <= 0 || targetFile.length() == total) &&
                        context.packageManager.getPackageArchiveInfo(targetFile.absolutePath, 0) != null

                if (isTargetValid) {
                    emit(DownloadState.Success(targetFile))
                    return@flow
                }

                if (tempFile.exists()) tempFile.delete()
                if (targetFile.exists()) targetFile.delete()

                var written = 0L
                var lastProgress = -1
                tempFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            if (total > 0) {
                                val progress = (written * 100 / total).toInt()
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    emit(DownloadState.Downloading(progress))
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (total > 0 && written != total) {
                    tempFile.delete()
                    emit(DownloadState.Failed("Incomplete download: expected $total bytes, wrote $written bytes. Please retry or download APK directly from GitHub."))
                    return@flow
                }

                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                emit(DownloadState.Success(targetFile))
            }
        } finally {
            if (call.isExecuted() && !call.isCanceled()) {
                call.cancel()
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }.flowOn(Dispatchers.IO).catch { e ->
        if (e !is CancellationException) {
            emit(DownloadState.Failed(e.message ?: "Download failed"))
        }
    }
}
