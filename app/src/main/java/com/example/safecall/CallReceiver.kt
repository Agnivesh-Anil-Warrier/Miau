package com.example.safecall

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"
        val callerNumber = intent.getStringExtra("CALLER_NUMBER") ?: ""

        Log.d("SafeCall", "CallReceiver received call alarm for $callerName ($callerNumber)")

        val callIntent = Intent(context, FakeCallActivity::class.java).apply {
            putExtra("CALLER_NAME", callerName)
            putExtra("CALLER_NUMBER", callerNumber)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            100,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CallNotification.CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(callerName)
            .setContentText(callerNumber)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (e: Exception) {
                Log.e("SafeCall", "Failed to post notification", e)
            }
        }

        try {
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Log.w("SafeCall", "Direct startActivity blocked by BAL (handled via Notification fullScreenIntent)", e)
        }
    }
}
