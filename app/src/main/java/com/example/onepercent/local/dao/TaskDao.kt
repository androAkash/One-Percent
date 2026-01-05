package com.example.onepercent.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.onepercent.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isPriority DESC, createdDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPriority = 1 ORDER BY createdDate ASC")
    fun getPriorityTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPriority= 0 ORDER BY createdDate ASC")
    fun getNormalTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isPriority = 0 AND isCompleted = 1 ORDER BY completedDate DESC")
    fun getCompletedNormalTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task : TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}