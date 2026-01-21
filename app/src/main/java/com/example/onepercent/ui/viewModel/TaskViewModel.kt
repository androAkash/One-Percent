package com.example.onepercent.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.onepercent.local.entity.TaskCompletionEntity
import com.example.onepercent.local.entity.TaskEntity
import com.example.onepercent.local.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TaskViewModel(val repository: TaskRepository): ViewModel() {
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
    fun addTask(name: String, isPriority: Boolean){
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

        return java.time.Period.between(createdDate, today).days.coerceAtLeast(0)
    }

    private fun getDateKey(timestamp: Long = System.currentTimeMillis()): Long {
        return Instant.ofEpochMilli(timestamp)
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