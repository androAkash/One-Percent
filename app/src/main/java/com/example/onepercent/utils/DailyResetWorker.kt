package com.example.onepercent.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.onepercent.local.repository.TaskRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyResetWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.checkAndResetPriorityTasks()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

fun scheduleDailyReset(context: Context) {
    val now = Calendar.getInstance()
    val resetTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
    }

    val delay = resetTime.timeInMillis - now.timeInMillis

    val request = OneTimeWorkRequestBuilder<DailyResetWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "daily_reset",
        ExistingWorkPolicy.REPLACE,
        request
    )
}