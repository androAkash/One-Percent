package com.example.onepercent.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.onepercent.local.entity.PriorityTaskEntity
import com.example.onepercent.local.entity.TaskEntity
import com.example.onepercent.local.repository.TaskRepository
import kotlinx.coroutines.delay
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
import java.util.concurrent.TimeUnit


class TaskViewModel(val repository: TaskRepository): ViewModel() {
    private val _priorityTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val priorityTask : StateFlow<List<TaskEntity>> = _priorityTask.asStateFlow()
    private val _normalTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val normalTask : StateFlow<List<TaskEntity>> = _normalTask.asStateFlow()

    private val _completedNormalTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val completedNormalTasks: StateFlow<List<TaskEntity>> = _completedNormalTasks.asStateFlow()

    private val _priorityCompletions = MutableStateFlow<List<PriorityTaskEntity>>(emptyList())
    val priorityCompletions: StateFlow<List<PriorityTaskEntity>> = _priorityCompletions.asStateFlow()

    private val _currentResetPeriod = MutableStateFlow(getCurrentResetPeriod())
    val currentResetPeriod: StateFlow<Long> = _currentResetPeriod.asStateFlow()

    private val RESET_HOUR = 0
    private val RESET_MINUTE = 0

    private var lastResetPeriod: Long? = null

    init {
        println("🕐 RESET TIME CONFIG: ${RESET_HOUR}:${RESET_MINUTE}")
        println("📅 App initialized at: ${ZonedDateTime.now(ZoneId.systemDefault())}")

        loadPriorityTask()
        loadNormalTask()
        completedNormalTask()

        viewModelScope.launch {
            repository.allCompletions.collect { completions ->
                _priorityCompletions.value = completions
            }
        }

        // Check immediately when app starts
        viewModelScope.launch {
            delay(200) // Give database time to initialize
            println("🚀 Performing initial reset check on app start")
            normalizeTasksIfDayChanged()
        }

        startPeriodicResetCheck()
    }

    private fun startPeriodicResetCheck() {
        viewModelScope.launch {
            while (true) {
                delay(60_000) // Check every 60 seconds

                _currentResetPeriod.value = getCurrentResetPeriod()
                normalizeTasksIfDayChanged()
            }
        }
    }

    fun normalizeTasksIfDayChanged() {
        viewModelScope.launch {
            val currentResetPeriod = getCurrentResetPeriod()

            println("🟢 Checking reset period at ${System.currentTimeMillis()}")
            println("  Current reset period: $currentResetPeriod")
            println("  Last reset period: $lastResetPeriod")

            if (lastResetPeriod == currentResetPeriod) {
                println("  ⏸️ Same period, no reset needed")
                return@launch
            }

            println("  ✅ NEW PERIOD DETECTED! Resetting tasks...")
            lastResetPeriod = currentResetPeriod
            _currentResetPeriod.value = currentResetPeriod

            // Query database directly for fresh data
            normalizePriorityTasks(currentResetPeriod)
        }
    }

    private suspend fun normalizePriorityTasks(currentResetPeriod: Long) {
        // IMPORTANT: Query database directly, not _priorityTask.value
        val currentTasks = repository.priorityTasks.first()

        println("🔴 Normalizing ${currentTasks.size} priority tasks")

        currentTasks.forEach { task ->
            println("  📋 Task: ${task.name} (id=${task.id}, completed=${task.isCompleted})")

            if (task.isCompleted) {
                val completedDate = task.completedDate

                if (completedDate == null) {
                    println("    ⚠️ No completedDate, unchecking")
                    repository.update(
                        task.copy(
                            isCompleted = false,
                            completedDate = null
                        )
                    )
                    return@forEach
                }

                val completedResetPeriod = getResetPeriodForTimestamp(completedDate)

                println("    ⏰ Completed period: $completedResetPeriod")
                println("    ⏰ Current period: $currentResetPeriod")

                if (completedResetPeriod < currentResetPeriod) {
                    println("    ✅ RESETTING '${task.name}'")
                    repository.update(
                        task.copy(
                            isCompleted = false,
                            completedDate = null
                        )
                    )
                } else {
                    println("    ⏸️ Still current period, keeping checked")
                }
            }
        }

        println("🔴 Normalization complete")
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
            if (existingCompletion != null) {
                repository.deleteCompletionForTaskOnDate(task.id, currentResetPeriod)
            }
            val updatedTask = task.copy(isCompleted = false, completedDate = null)
            repository.update(updatedTask)
        } else {
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

    fun calculatePendingDays(taskCreatedDate: Long): Int {
        val currentResetPeriod = getCurrentResetPeriod()
        val taskCreatedResetPeriod = getResetPeriodForTimestamp(taskCreatedDate)

        val diffInMillis = currentResetPeriod - taskCreatedResetPeriod
        val daysDiff = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

        return maxOf(0, daysDiff)
    }
    private fun getCurrentResetPeriod(): Long {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val todayReset = now.toLocalDate().atTime(RESET_HOUR, RESET_MINUTE)
            .atZone(ZoneId.systemDefault())

        return if (now.isBefore(todayReset)) {
            todayReset.minusDays(1).toInstant().toEpochMilli()
        } else {
            todayReset.toInstant().toEpochMilli()
        }
    }

    private fun getResetPeriodForTimestamp(timestamp: Long): Long {
        val time = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())

        val dateReset = time.toLocalDate().atTime(RESET_HOUR, RESET_MINUTE)
            .atZone(ZoneId.systemDefault())

        return if (time.isBefore(dateReset)) {
            dateReset.minusDays(1).toInstant().toEpochMilli()
        } else {
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