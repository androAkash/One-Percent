package com.example.onepercent.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.onepercent.database.TaskDatabase
import com.example.onepercent.local.repository.TaskRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class DailyResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = TaskDatabase.getDatabase(applicationContext)
            val repository = TaskRepository(
                taskDao = database.taskDao(),
                taskCompletionDao = database.priorityCompletionDao()
            )
            repository.checkAndResetPriorityTasks()
            Log.d("WorkManager", "Reset completed successfully")
            scheduleDailyReset(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("WorkManager", "Reset failed", e)
            Result.retry()
        }
    }
}

fun scheduleDailyReset(context: Context) {
    val now = LocalDateTime.now()
    val nextMidnight = now.toLocalDate()
        .plusDays(1)
        .atStartOfDay()

    val delayMillis = Duration.between(now, nextMidnight).toMillis()

    Log.d("WorkScheduler", "Scheduling reset in ${delayMillis / 1000 / 60} minutes")

    val request = OneTimeWorkRequestBuilder<DailyResetWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag("daily_reset")
        .build()

    WorkManager.getInstance(context)
        .enqueueUniqueWork(
            "daily_reset",
            ExistingWorkPolicy.REPLACE,
            request
        )
}