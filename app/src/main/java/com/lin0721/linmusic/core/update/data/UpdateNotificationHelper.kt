package com.lin0721.linmusic.core.update.data

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

private const val TAG = "UpdateNotification"
private const val CHANNEL_ID = "app_update"
private const val NOTIFICATION_ID = 8001

// App Updates： and Download in background、Install
class UpdateNotificationHelper(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "App update download progress and install notifications"
                setShowBadge(false)
            }
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            systemManager?.createNotificationChannel(channel)
        }
    }

    fun showDownloading(versionName: String, progress: Int) {
        if (!notificationManager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Downloading new version Mimikyu $versionName")
            .setContentText("Downloaded $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "No permission to post download progress notification", e)
        }
    }

    fun showDownloadSuccess(versionName: String, installIntent: Intent) {
        if (!notificationManager.areNotificationsEnabled()) return

        val installPendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mimikyu $versionName download complete")
            .setContentText("Tap to install now")
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(installPendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "No permission to post download success notification", e)
        }
    }

    fun showDownloadFailed(error: String) {
        if (!notificationManager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Failed to download new version")
            .setContentText(error)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            AppLogger.e(TAG, "No permission to post download failed notification", e)
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
