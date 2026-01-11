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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId


class TaskViewModel(val repository: TaskRepository): ViewModel() {
    private val _priorityTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val priorityTask : StateFlow<List<TaskEntity>> = _priorityTask.asStateFlow()
    private val _normalTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val normalTask : StateFlow<List<TaskEntity>> = _normalTask.asStateFlow()

    private val _completedNormalTasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val completedNormalTasks: StateFlow<List<TaskEntity>> = _completedNormalTasks.asStateFlow()

    private val _priorityCompletions = MutableStateFlow<List<PriorityTaskEntity>>(emptyList())
    val priorityCompletions: StateFlow<List<PriorityTaskEntity>> = _priorityCompletions.asStateFlow()

    init {
        loadPriorityTask()
        loadNormalTask()
        completedNormalTask()

        viewModelScope.launch {
            repository.allCompletions.collect { completions ->
                _priorityCompletions.value = completions
            }
        }
        checkAndResetPriorityTasks()
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
        val today = getTodayAtMidnight()
        val existingCompletion = repository.getCompletionForTaskOnDate(task.id, today)

        if (task.isCompleted) {
            // Unchecking - remove completion record
            if (existingCompletion != null) {
                repository.deleteCompletionForTaskOnDate(task.id, today)
            }
            val updatedTask = task.copy(isCompleted = false, completedDate = null)
            repository.update(updatedTask)
        } else {
            // Checking - add completion record
            val completion = PriorityTaskEntity(
                taskId = task.id,
                taskName = task.name,
                completedDate = today
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

    private fun checkAndResetPriorityTasks() {
        viewModelScope.launch {
            val today = getTodayAtMidnight()
            val tasks = _priorityTask.value

            tasks.forEach { task ->
                if (task.isCompleted) {
                    val completedDate = task.completedDate ?: return@forEach
                    val completedDay = getDateAtMidnight(completedDate)

                    // If task was completed on a different day, reset it
                    if (completedDay < today) {
                        val resetTask = task.copy(isCompleted = false, completedDate = null)
                        repository.update(resetTask)
                    }
                }
            }
        }
    }
    fun clearHeatmapData() {
        viewModelScope.launch {
            repository.clearAllCompletions()
        }
    }
    private fun getTodayAtMidnight(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun getDateAtMidnight(timestamp: Long): Long {
        return java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
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