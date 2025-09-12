package com.aidestinymaster.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aidestinymaster.app.R

object Notifications {
    const val CHANNEL_ID = "ANALYSIS_CHANNEL"
    private const val CHANNEL_NAME = "AI 分析"
    private const val CHANNEL_DESC = "AI 背景分析與生成通知"

    fun ensureAnalysisChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
                ch.description = CHANNEL_DESC
                ch.enableLights(true)
                ch.lightColor = Color.BLUE
                ch.enableVibration(false)
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun buildProgress(context: Context, title: String, text: String, progress: Int?, ongoing: Boolean = true): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .apply { progress?.let { setProgress(100, it.coerceIn(0, 100), false) } }
            .build()
    }

    fun buildCompleted(context: Context, title: String, text: String, deepLink: Uri? = null): Notification {
            val pi: PendingIntent? = deepLink?.let {
                val intent = Intent(Intent.ACTION_VIEW, it).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                )
            }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
    }
}
