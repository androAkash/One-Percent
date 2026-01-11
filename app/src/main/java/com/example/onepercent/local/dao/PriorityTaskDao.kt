package com.example.onepercent.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.onepercent.local.entity.PriorityTaskEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface PriorityTaskDao {
    @Insert
    suspend fun insertCompletion(completion: PriorityTaskEntity)
    @Delete
    suspend fun deleteCompletion(completion: PriorityTaskEntity)

    @Query("SELECT * FROM priority_task ORDER BY completedDate DESC")
    fun getAllCompletions(): Flow<List<PriorityTaskEntity>>
    @Query("SELECT * FROM priority_task WHERE taskId = :taskId AND completedDate = :date")
    suspend fun getCompletionForTaskOnDate(taskId: Int, date: Long): PriorityTaskEntity?
    @Query("DELETE FROM priority_task WHERE taskId = :taskId AND completedDate = :date")
    suspend fun deleteCompletionForTaskOnDate(taskId: Int, date: Long)
    @Query("DELETE FROM priority_task WHERE taskId = :taskId")
    suspend fun deleteAllCompletionsForTask(taskId: Int)
    @Query("DELETE FROM priority_task")
    suspend fun clearAllCompletions()
}