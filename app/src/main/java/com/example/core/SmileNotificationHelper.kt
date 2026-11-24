package com.example.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object SmileNotificationHelper {
    const val CHANNEL_MESSAGES = "smile_messages"
    const val CHANNEL_CALLS = "smile_calls"
    const val CHANNEL_UPDATES = "smile_updates"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "SMILE Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time 1-to-1 and group chat messages"
            }
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "SMILE Voice & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice and video calls"
            }
            val updateChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "SMILE Status & Communities",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status updates and community announcements"
            }
            nm.createNotificationChannels(listOf(msgChannel, callChannel, updateChannel))
        }
    }

    fun showNotification(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        body: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }
}
