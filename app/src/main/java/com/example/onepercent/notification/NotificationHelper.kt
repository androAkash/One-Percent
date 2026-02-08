package com.example.onepercent.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.onepercent.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "task_reminder_channel"
    private const val CHANNEL_NAME = "Task Reminders"
    private const val CHANNEL_DESCRIPTION = "Daily reminders for your priority tasks"

    fun createNotificationChannel(context: Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    /**
     * Shows a notification for a task reminder
     */
    fun showTaskReminder(
        context: Context,
        taskId: Int,
        taskName: String
    ) {
        // Intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.alert_light_frame) // Replace with your app icon
            .setContentTitle("🎯 Priority Task Reminder")
            .setContentText(taskName)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Don't forget: $taskName\n\nComplete your 1% improvement today!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        // Show notification
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(taskId, notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    /**
     * Cancels a specific task notification
     */
    fun cancelNotification(context: Context, taskId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(taskId)
    }
}