package com.example.onepercent.local.repository

import com.example.onepercent.local.dao.TaskDao
import com.example.onepercent.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao : TaskDao) {
    val allTask : Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val priorityTasks: Flow<List<TaskEntity>> = taskDao.getPriorityTasks()
    val normalTasks: Flow<List<TaskEntity>> = taskDao.getNormalTasks()

    suspend fun insert(taskEntity: TaskEntity){
        taskDao.insertTask(taskEntity)
    }
    suspend fun update(taskEntity: TaskEntity){
        taskDao.updateTask(taskEntity)
    }
    suspend fun delete(taskEntity: TaskEntity){
        taskDao.deleteTask(taskEntity)
    }
    suspend fun deleteAll(){
        taskDao.deleteAll()
    }
}