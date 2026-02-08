package com.example.onepercent.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.onepercent.database.TaskDatabase
import com.example.onepercent.notification.NotificationHelper
import com.example.onepercent.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskReminderWorker (context: Context,
    workerParameters: WorkerParameters): CoroutineWorker(context,workerParameters){
    override suspend fun doWork(): Result {
        return try {
            val taskId = inputData.getInt(KEY_TASK_ID,-1)
            val taskName = inputData.getString(KEY_TASK_NAME) ?: return Result.failure()
            val hour = inputData.getInt(KEY_HOUR,-1)
            val minute = inputData.getInt(KEY_MINUTE,-1)

            if (taskId == -1 || hour == - 1 || minute == -1){
                Log.e(TAG, "Invalid input data")
                return Result.failure()
            }
            // Check if task still exists and has reminder enabled
            val database = TaskDatabase.getDatabase(applicationContext)
            val task = withContext(Dispatchers.IO){ database.taskDao().getTaskById(taskId) }
            if (task != null && task.isReminderEnabled && task.isPriority){
                NotificationHelper.showTaskReminder(
                    context = applicationContext,
                    taskId = taskId,
                    taskName = taskName
                )
                Log.d(TAG, "Notification shown for task: $taskName")

                //Reschedule for next day
                NotificationScheduler.scheduleTaskReminder(
                    context = applicationContext,
                    taskId = taskId,
                    taskName = taskName,
                    hour = hour,
                    minute = minute
                )
                Log.d(TAG, "Rescheduled reminder for task: $taskName")
            } else {
                Log.d(TAG, "Task not found or reminder disabled, stopping notifications")
            }

            Result.success()
        } catch (e: Exception){
            Log.e(TAG, "Error in TaskReminderWorker", e)
            Result.retry()
        }
    }
    companion object {
        private const val TAG = "TaskReminderWorker"
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_NAME = "task_name"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}