package com.example.onepercent.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.onepercent.local.entity.TaskEntity
import com.example.onepercent.local.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class TaskViewModel(val repository: TaskRepository): ViewModel() {
    private val _priorityTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val priorityTask : StateFlow<List<TaskEntity>> = _priorityTask.asStateFlow()
    private val _normalTask = MutableStateFlow<List<TaskEntity>>(emptyList())
    val normalTask : StateFlow<List<TaskEntity>> = _normalTask.asStateFlow()

    init {
        loadPriorityTask()
        loadNormalTask()
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
    fun deleteTask(taskEntity: TaskEntity){
        viewModelScope.launch {
            repository.delete(taskEntity)
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