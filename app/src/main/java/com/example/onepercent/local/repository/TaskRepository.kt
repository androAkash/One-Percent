package com.example.onepercent.local.repository

import com.example.onepercent.local.dao.PriorityTaskDao
import com.example.onepercent.local.dao.TaskDao
import com.example.onepercent.local.entity.PriorityTaskEntity
import com.example.onepercent.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val priorityCompletionDao: PriorityTaskDao
) {
    val allTask : Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val priorityTasks: Flow<List<TaskEntity>> = taskDao.getPriorityTasks()
    val normalTasks: Flow<List<TaskEntity>> = taskDao.getNormalTasks()
    val completedNormalTasks: Flow<List<TaskEntity>> = taskDao.getCompletedNormalTasks()
    val allCompletions: Flow<List<PriorityTaskEntity>> = priorityCompletionDao.getAllCompletions()

    suspend fun insert(taskEntity: TaskEntity){
        taskDao.insertTask(taskEntity)
    }
    suspend fun update(taskEntity: TaskEntity){
        taskDao.updateTask(taskEntity)
    }
    suspend fun insertCompletion(completion: PriorityTaskEntity) {
        priorityCompletionDao.insertCompletion(completion)
    }
    suspend fun deleteCompletionForTaskOnDate(taskId: Int, date: Long) {
        priorityCompletionDao.deleteCompletionForTaskOnDate(taskId, date)
    }
    suspend fun getCompletionForTaskOnDate(taskId: Int, date: Long): PriorityTaskEntity? {
        return priorityCompletionDao.getCompletionForTaskOnDate(taskId, date)
    }
    suspend fun delete(taskEntity: TaskEntity){
        taskDao.deleteTask(taskEntity)
    }
    suspend fun deleteAll(){
        taskDao.deleteAll()
    }
    suspend fun clearAllCompletions() {
        priorityCompletionDao.clearAllCompletions()
    }
}