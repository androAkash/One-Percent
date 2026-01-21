package com.example.onepercent.local.repository

import com.example.onepercent.local.dao.TaskCompletionDao
import com.example.onepercent.local.dao.TaskDao
import com.example.onepercent.local.entity.TaskCompletionEntity
import com.example.onepercent.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaskRepository(
    private val taskDao: TaskDao,
    private val taskCompletionDao: TaskCompletionDao
) {
    val allTask : Flow<List<TaskEntity>> = taskDao.getAllTasks() //TODO: Dont know the meaning of this variable
    val priorityTasks: Flow<List<TaskEntity>> = taskDao.getPriorityTasks()
    val normalTasks: Flow<List<TaskEntity>> = taskDao.getNormalTasks()
    val completedNormalTasks: Flow<List<TaskEntity>> = taskDao.getCompletedNormalTasks()
    val allCompletions: Flow<List<TaskCompletionEntity>> = taskCompletionDao.getAllCompletions() //TODO:  May be priorityTaskCompleted

    suspend fun checkAndResetPriorityTasks() {
        val tasks = taskDao.getPriorityTasks().first()
        val today = LocalDate.now()

        tasks.forEach { task ->
            if (task.isCompleted && !isCompletedToday(task.completedDate, today)) {
                taskDao.updateTask(
                    task.copy(
                        isCompleted = false,
                        completedDate = null
                    )
                )
            }
        }
    }
    private fun isCompletedToday(completedTimestamp: Long?, today: LocalDate): Boolean {
        if (completedTimestamp == null) return false
        val completedDate = Instant.ofEpochMilli(completedTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return completedDate == today
    }
    suspend fun insert(taskEntity: TaskEntity){
        taskDao.insertTask(taskEntity)
    }
    suspend fun update(taskEntity: TaskEntity){
        taskDao.updateTask(taskEntity)
    }
    suspend fun insertCompletion(completion: TaskCompletionEntity) {
        taskCompletionDao.insertCompletion(completion)
    }
    suspend fun deleteCompletionForTaskOnDate(taskId: Int, date: Long) {
        taskCompletionDao.deleteCompletionForTaskOnDate(taskId, date)
    }
    suspend fun getCompletionForTaskOnDate(taskId: Int, date: Long): TaskCompletionEntity? {
        return taskCompletionDao.getCompletionForTaskOnDate(taskId, date)
    }
    suspend fun delete(taskEntity: TaskEntity){
        taskDao.deleteTask(taskEntity)
    }
    suspend fun deleteAll(){
        taskDao.deleteAll()
    }
    suspend fun clearAllCompletions() {
        taskCompletionDao.clearAllCompletions()
    }
}