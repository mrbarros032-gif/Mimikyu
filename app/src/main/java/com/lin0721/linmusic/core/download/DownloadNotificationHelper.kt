package com.lin0721.linmusic.core.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lin0721.linmusic.MainActivity
import com.lin0721.linmusic.R
import com.lin0721.linmusic.core.log.AppLogger

private const val TAG = "DownloadNotification"
private const val CHANNEL_ID = "song_download"
private const val NOTIFICATION_ID_BASE = 9000
// and  ID
private const val TERMINAL_NOTIFICATION_ID_BASE = 19000
private const val BATCH_NOTIFICATION_ID_BASE = 29000
private const val BATCH_TERMINAL_NOTIFICATION_ID_BASE = 39000
private const val NOTIFICATION_ID_SLOTS = 1000

// SongsDownload
class DownloadNotificationHelper(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Song Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Song download progress and notifications"
                setShowBadge(false)
            }
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            systemManager?.createNotificationChannel(channel)
        }
    }

    fun notificationIdFor(songId: Long): Int =
        NOTIFICATION_ID_BASE + (songId % NOTIFICATION_ID_SLOTS).toInt()

    private fun terminalNotificationIdFor(songId: Long): Int =
        TERMINAL_NOTIFICATION_ID_BASE + (songId % NOTIFICATION_ID_SLOTS).toInt()

    fun batchNotificationIdFor(batchTag: String): Int =
        BATCH_NOTIFICATION_ID_BASE + (Math.floorMod(batchTag.hashCode(), NOTIFICATION_ID_SLOTS))

    private fun batchTerminalNotificationIdFor(batchTag: String): Int =
        BATCH_TERMINAL_NOTIFICATION_ID_BASE + (Math.floorMod(batchTag.hashCode(), NOTIFICATION_ID_SLOTS))

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    fun buildProgressNotification(songName: String, progress: Int) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading $songName")
            .setContentText(if (progress > 0) "$progress% downloaded" else "Preparing download…")
            .setProgress(100, progress, progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()

    // Download
    fun buildBatchProgressNotification(batchLabel: String, settled: Int, total: Int) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading $batchLabel")
            .setContentText("$settled/$total completed")
            .setProgress(total.coerceAtLeast(1), settled, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()

    fun showSuccess(songId: Long, songName: String) {
        if (!notificationManager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$songName downloaded")
            .setContentText("Saved to Music/October")
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        notify(terminalNotificationIdFor(songId), notification)
    }

    fun showFailed(songId: Long, songName: String, reason: String) {
        if (!notificationManager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$songName download failed")
            .setContentText(reason)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        notify(terminalNotificationIdFor(songId), notification)
    }

    // Download
    fun showBatchSummary(batchTag: String, batchLabel: String, total: Int) {
        if (!notificationManager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$batchLabel downloaded")
            .setContentText("$total songs saved to Music/October/$batchLabel")
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        notify(batchTerminalNotificationIdFor(batchTag), notification)
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "No permission to post download notification", e)
        }
    }
}
