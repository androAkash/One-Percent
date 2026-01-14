package com.example.onepercent.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.onepercent.local.entity.PriorityTaskEntity
import com.example.onepercent.local.entity.TaskEntity
import com.example.onepercent.local.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime


class TaskViewModel(val repository: TaskRepository): ViewModel() {
    private val _priorityTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val priorityTask : StateFlow<List<TaskEntity>> = _priorityTask.asStateFlow()
    private val _normalTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val normalTask : StateFlow<List<TaskEntity>> = _normalTask.asStateFlow()

    private val _completedNormalTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val completedNormalTasks: StateFlow<List<TaskEntity>> = _completedNormalTasks.asStateFlow()

    private val _priorityCompletions = MutableStateFlow<List<PriorityTaskEntity>>(emptyList())
    val priorityCompletions: StateFlow<List<PriorityTaskEntity>> = _priorityCompletions.asStateFlow()

    private val RESET_HOUR = 22 // 0 = midnight (00:00), 20 = 8 PM, etc.
    private val RESET_MINUTE = 7

    // Store the last reset period timestamp, not just "today"
    private var lastResetPeriod: Long? = null


    init {
        loadPriorityTask()
        loadNormalTask()
        completedNormalTask()

        viewModelScope.launch {
            repository.allCompletions.collect { completions ->
                _priorityCompletions.value = completions
            }
        }
        viewModelScope.launch {
            // Wait for priority tasks to load
            _priorityTask.first { it.isNotEmpty() }
            normalizeTasksIfDayChanged()
        }
    }

    fun normalizeTasksIfDayChanged() {
        viewModelScope.launch {
            val currentResetPeriod = getCurrentResetPeriod()

            // Check if we've entered a new reset period
            if (lastResetPeriod == currentResetPeriod) return@launch

            lastResetPeriod = currentResetPeriod

            normalizePriorityTasks(currentResetPeriod)
        }
    }

    private suspend fun normalizePriorityTasks(currentResetPeriod: Long) {
        val currentTasks = _priorityTask.value

        currentTasks.forEach { task ->
            if (task.isCompleted) {
                val completedDate = task.completedDate ?: return@forEach
                val completedResetPeriod = getResetPeriodForTimestamp(completedDate)
                if (completedResetPeriod < currentResetPeriod) {
                    repository.update(
                        task.copy(
                            isCompleted = false,
                            completedDate = null
                        )
                    )
                }
            }
        }
    }

    private fun loadPriorityTask(){
        viewModelScope.launch {
            repository.priorityTasks.collect { tasks ->
                println("🔵 Priority tasks updated: ${tasks.size} tasks")
                tasks.forEach {
                    println("  - ${it.name}: isCompleted=${it.isCompleted}")
                }
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

    private fun completedNormalTask(){
        viewModelScope.launch {
            repository.completedNormalTasks.collect { task->
                _completedNormalTasks.value = task
            }
        }
    }

    fun addTask(name: String,isPriority: Boolean){
        viewModelScope.launch {
            val task = TaskEntity(name = name, isPriority = isPriority)
            repository.insert(task)
        }
    }

    fun updateTask(taskEntity: TaskEntity){
        viewModelScope.launch {
            repository.update(taskEntity)
        }
    }

    private suspend fun togglePriorityTaskCompletion(task: TaskEntity) {
        val currentResetPeriod = getCurrentResetPeriod()
        val existingCompletion = repository.getCompletionForTaskOnDate(task.id, currentResetPeriod)

        if (task.isCompleted) {
            // Unchecking - remove completion record
            if (existingCompletion != null) {
                repository.deleteCompletionForTaskOnDate(task.id, currentResetPeriod)
            }
            val updatedTask = task.copy(isCompleted = false, completedDate = null)
            repository.update(updatedTask)
        } else {
            // Checking - add completion record
            val completion = PriorityTaskEntity(
                taskId = task.id,
                taskName = task.name,
                completedDate = currentResetPeriod
            )
            repository.insertCompletion(completion)
            val updatedTask = task.copy(isCompleted = true, completedDate = System.currentTimeMillis())
            repository.update(updatedTask)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            if (task.isPriority) {
                // Handle priority task completion with heatmap tracking
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

    fun forceResetPriorityTasks() {
        viewModelScope.launch {
            val currentTasks = _priorityTask.value
            currentTasks.forEach { task ->
                if (task.isCompleted) {
                    repository.update(
                        task.copy(
                            isCompleted = false,
                            completedDate = null
                        )
                    )
                }
            }
        }
    }

    fun clearHeatmapData() {
        viewModelScope.launch {
            repository.clearAllCompletions()
        }
    }

    /**
     * Get the current reset period timestamp.
     * This represents the most recent reset time that has passed.
     *
     * Example with RESET_HOUR = 22 (10 PM):
     * - If current time is Jan 15, 9 PM → returns Jan 14, 10 PM
     * - If current time is Jan 15, 11 PM → returns Jan 15, 10 PM
     * - If current time is Jan 16, 1 AM → returns Jan 15, 10 PM
     */
    private fun getCurrentResetPeriod(): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val todayReset = now.toLocalDate().atTime(RESET_HOUR, RESET_MINUTE)
            .atZone(ZoneId.systemDefault())

        return if (now.isBefore(todayReset)) {
            // Before today's reset time, use yesterday's reset
            todayReset.minusDays(1).toInstant().toEpochMilli()
        } else {
            // After today's reset time, use today's reset
            todayReset.toInstant().toEpochMilli()
        }
    }

    /**
     * Get which reset period a timestamp belongs to.
     *
     * Example with RESET_HOUR = 22 (10 PM):
     * - Jan 15, 9 PM → belongs to Jan 14, 10 PM period
     * - Jan 15, 11 PM → belongs to Jan 15, 10 PM period
     */
    private fun getResetPeriodForTimestamp(timestamp: Long): Long {
        val time = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())

        val dateReset = time.toLocalDate().atTime(RESET_HOUR, RESET_MINUTE)
            .atZone(ZoneId.systemDefault())

        return if (time.isBefore(dateReset)) {
            // Before that day's reset time, belongs to previous day's period
            dateReset.minusDays(1).toInstant().toEpochMilli()
        } else {
            // After that day's reset time, belongs to that day's period
            dateReset.toInstant().toEpochMilli()
        }
    }
}
class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}