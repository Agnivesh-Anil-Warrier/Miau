package com.example.safecall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object CallNotification {

    const val CHANNEL_ID = "fake_call_channel"

    fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fake Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming fake call notifications"
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }
}