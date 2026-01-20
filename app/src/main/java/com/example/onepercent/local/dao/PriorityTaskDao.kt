package com.example.onepercent.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.onepercent.local.entity.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface PriorityTaskDao {
    @Insert
    suspend fun insertCompletion(completion: TaskCompletionEntity)
    @Delete
    suspend fun deleteCompletion(completion: TaskCompletionEntity) //TODO: Why its created? and what was the purpose

    @Query("SELECT * FROM priority_task ORDER BY completedDate DESC")
    fun getAllCompletions(): Flow<List<TaskCompletionEntity>>
    @Query("SELECT * FROM priority_task WHERE taskId = :taskId AND completedDate = :date")
    suspend fun getCompletionForTaskOnDate(taskId: Int, date: Long): TaskCompletionEntity?
    @Query("DELETE FROM priority_task WHERE taskId = :taskId AND completedDate = :date")
    suspend fun deleteCompletionForTaskOnDate(taskId: Int, date: Long)
    @Query("DELETE FROM priority_task WHERE taskId = :taskId")
    suspend fun deleteAllCompletionsForTask(taskId: Int) //TODO: Why its created? and what was the purpose
    @Query("DELETE FROM priority_task")
    suspend fun clearAllCompletions()
}