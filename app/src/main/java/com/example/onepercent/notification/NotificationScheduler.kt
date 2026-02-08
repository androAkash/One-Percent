package com.example.onepercent.notification

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.onepercent.local.dao.TaskDao
import com.example.onepercent.utils.TaskReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    /**
     * Calculates delay until the next occurrence of the specified time
     * If the time has already passed today, schedules for tomorrow
     */
    private fun calculateDelayUntilTime(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val targetTime = LocalTime.of(hour, minute)

        var targetDateTime = now.toLocalDate().atTime(targetTime)

        // If time has already passed today, schedule for tomorrow
        if (now.toLocalTime().isAfter(targetTime)) {
            targetDateTime = targetDateTime.plusDays(1)
        }

        val delayMillis = Duration.between(now, targetDateTime).toMillis()

        Log.d(TAG, "Scheduling notification in ${delayMillis / 1000 / 60} minutes for $hour:$minute")

        return delayMillis
    }

    /**
     * Schedules a daily reminder for a task at the specified time
     */
    fun scheduleTaskReminder(
        context: Context,
        taskId: Int,
        taskName: String,
        hour: Int,
        minute: Int
    ) {
        val delayMillis = calculateDelayUntilTime(hour, minute)

        val inputData = Data.Builder()
            .putInt(TaskReminderWorker.KEY_TASK_ID, taskId)
            .putString(TaskReminderWorker.KEY_TASK_NAME, taskName)
            .putInt(TaskReminderWorker.KEY_HOUR, hour)
            .putInt(TaskReminderWorker.KEY_MINUTE, minute)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(getWorkTag(taskId))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                getWorkName(taskId),
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        Log.d(TAG, "Scheduled reminder for task $taskId: $taskName at $hour:$minute")
    }

    /**
     * Cancels the reminder for a specific task
     */
    fun cancelTaskReminder(context: Context, taskId: Int) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(getWorkName(taskId))

        // Also cancel any shown notification
        NotificationHelper.cancelNotification(context, taskId)

        Log.d(TAG, "Cancelled reminder for task $taskId")
    }
    /**
     * Reschedules all active reminders (call this on app startup)
     */
    suspend fun rescheduleAllReminders(context: Context, taskDao: TaskDao) {
        val tasks = taskDao.getAllTasksOneTime()

        tasks.filter { it.isPriority && it.isReminderEnabled }
            .forEach { task ->
                task.reminderHour?.let { hour ->
                    task.reminderMinute?.let { minute ->
                        scheduleTaskReminder(
                            context = context,
                            taskId = task.id,
                            taskName = task.name,
                            hour = hour,
                            minute = minute
                        )
                    }
                }
            }

        Log.d(TAG, "Rescheduled all active reminders")
    }
    private fun getWorkName(taskId: Int) = "task_reminder_$taskId"
    private fun getWorkTag(taskId: Int) = "reminder_$taskId"
}