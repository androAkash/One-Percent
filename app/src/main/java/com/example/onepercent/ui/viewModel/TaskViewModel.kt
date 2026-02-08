package com.example.onepercent.ui.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.onepercent.local.entity.TaskCompletionEntity
import com.example.onepercent.local.entity.TaskEntity
import com.example.onepercent.local.repository.TaskRepository
import com.example.onepercent.notification.NotificationScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId

class TaskViewModel(
    val repository: TaskRepository,
    private val context: Context
) : ViewModel() {
    private val _priorityTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val priorityTask : StateFlow<List<TaskEntity>> = _priorityTask.asStateFlow()

    private val _normalTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val normalTask : StateFlow<List<TaskEntity>> = _normalTask.asStateFlow()

    private val _completedNormalTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val completedNormalTasks: StateFlow<List<TaskEntity>> = _completedNormalTasks.asStateFlow()

    private val _completionHistory = MutableStateFlow<List<TaskCompletionEntity>>(emptyList())
    val completionHistory: StateFlow<List<TaskCompletionEntity>> = _completionHistory.asStateFlow()

    init {
        loadPriorityTask()
        loadNormalTask()
        loadCompletedNormalTask()
        loadCompletionHistory()

        viewModelScope.launch {
            repository.checkAndResetPriorityTasks()
            rescheduleAllReminders()
        }
    }
    private fun loadPriorityTask(){
        viewModelScope.launch {
            repository.priorityTasks.collect { tasks ->
                _priorityTask.value = tasks
            }
        }
    }
    private fun loadNormalTask(){
        viewModelScope.launch {
            repository.normalTasks.collect { tasks ->
                _normalTask.value = tasks
            }
        }
    }
    private fun loadCompletedNormalTask(){
        viewModelScope.launch {
            repository.completedNormalTasks.collect { tasks ->
                _completedNormalTasks.value = tasks
            }
        }
    }
    private fun loadCompletionHistory(){
        viewModelScope.launch {
            repository.allCompletions.collect { completions ->
                _completionHistory.value = completions
            }
        }
    }
    fun addTask(
        name: String,
        isPriority: Boolean,
        reminderHour: Int? = null,
        reminderMinute: Int? = null
    ) {
        viewModelScope.launch {
            // Auto-infer if reminder is enabled
            val isReminderEnabled = isPriority && reminderHour != null && reminderMinute != null

            val task = TaskEntity(
                name = name,
                isPriority = isPriority,
                isReminderEnabled = isReminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )

            val taskId = repository.insert(task)

            // Schedule notification if reminder is enabled
            if (isReminderEnabled) {
                NotificationScheduler.scheduleTaskReminder(
                    context = context,
                    taskId = taskId.toInt(),
                    taskName = name,
                    hour = reminderHour!!,
                    minute = reminderMinute!!
                )
            }
        }
    }
    fun updateTask(taskEntity: TaskEntity){
        viewModelScope.launch {
            repository.update(taskEntity)
        }
        if (taskEntity.isPriority && taskEntity.isReminderEnabled
            && taskEntity.reminderHour != null && taskEntity.reminderMinute != null){
            NotificationScheduler.scheduleTaskReminder(
                context = context,
                taskId = taskEntity.id,
                taskName = taskEntity.name,
                hour = taskEntity.reminderHour,
                minute = taskEntity.reminderMinute
            )
        } else {
            NotificationScheduler.cancelTaskReminder(
                context = context,
                taskId = taskEntity.id
            )
        }
    }
    private suspend fun togglePriorityTaskCompletion(task: TaskEntity) {
        val today = getDateKey(System.currentTimeMillis())
        val existingCompletion = repository.getCompletionForTaskOnDate(task.id, today)

        if (task.isCompleted) {
            // Unchecking the task
            if (existingCompletion != null) {
                repository.deleteCompletionForTaskOnDate(task.id, today)
            }
            repository.update(task.copy(isCompleted = false, completedDate = null))
        } else {
            // Checking the task
            val completion = TaskCompletionEntity(
                taskId = task.id,
                taskName = task.name,
                completedDate = today
            )
            repository.insertCompletion(completion)
            repository.update(task.copy(isCompleted = true, completedDate = System.currentTimeMillis()))
        }
    }
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            if (task.isPriority) {
                togglePriorityTaskCompletion(task)
            } else {
                val updatedTask = task.copy(
                    isCompleted = !task.isCompleted,
                    completedDate = if (!task.isCompleted) System.currentTimeMillis() else null
                )
                repository.update(updatedTask)
            }
        }
    }
    fun deleteTask(taskEntity: TaskEntity){
        viewModelScope.launch {
            repository.delete(taskEntity)
        }
    }
    fun clearHeatmapData() {
        viewModelScope.launch {
            repository.clearAllCompletions()
        }
    }

    fun calculatePendingDays(taskCreatedDate: Long): Int {
        val createdDate = Instant.ofEpochMilli(taskCreatedDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val today = LocalDate.now()

        return Period.between(createdDate, today).days.coerceAtLeast(0)
    }

    private fun getDateKey(timestamp: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private suspend fun rescheduleAllReminders(){
        NotificationScheduler.rescheduleAllReminders(context, taskDao = repository.taskDao)
    }
}
class TaskViewModelFactory(private val repository: TaskRepository,
    private val context : Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository,context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}