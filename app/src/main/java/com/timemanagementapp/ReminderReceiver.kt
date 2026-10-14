package com.timemanagementapp

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (notificationsAllowed) {
            val ringingIntent = Intent(context, RingingAlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val ringingScreen = PendingIntent.getActivity(
                context,
                ALARM_REQUEST_CODE,
                ringingIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val stopIntent = Intent(context, RingingAlarmActivity::class.java)
                .setAction(STOP_ALARM_ACTION)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val stopAlarm = PendingIntent.getActivity(
                context,
                ALARM_REQUEST_CODE + 1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Your alarm is ringing")
                .setContentText("Tap to open the alarm screen or stop the sound.")
                .setContentIntent(ringingScreen)
                .setFullScreenIntent(ringingScreen, true)
                .addAction(android.R.drawable.ic_media_pause, "Stop alarm", stopAlarm)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(ALARM_REQUEST_CODE, notification)
        }

        context.getSharedPreferences("time_management_app", Context.MODE_PRIVATE)
            .edit()
            .remove(ALARM_PREFERENCES_KEY)
            .apply()
    }
}
