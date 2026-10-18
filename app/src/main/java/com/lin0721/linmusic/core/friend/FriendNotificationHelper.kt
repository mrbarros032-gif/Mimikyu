package com.lin0721.linmusic.core.friend

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

private const val TAG = "FriendNotificationHelper"
private const val CHANNEL_ID = "friend_updates"
private const val CHANNEL_NAME = "Friend Updates & Recommendations"

class FriendNotificationHelper(private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about friends' new playlists, followed artists, and saved albums"
                enableLights(true)
                setShowBadge(true)
            }
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            systemManager?.createNotificationChannel(channel)
        }
    }

    fun showFriendNotification(notification: FriendNotification) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "friends_activity")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)

            val notifId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager.notify(notifId, builder.build())
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to post friend notification", e)
        }
    }
}
