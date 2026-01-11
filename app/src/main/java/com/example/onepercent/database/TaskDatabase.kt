package com.example.onepercent.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.onepercent.local.dao.PriorityTaskDao
import com.example.onepercent.local.dao.TaskDao
import com.example.onepercent.local.entity.PriorityTaskEntity
import com.example.onepercent.local.entity.TaskEntity

@Database(entities = [TaskEntity::class, PriorityTaskEntity::class], version = 4, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun priorityCompletionDao(): PriorityTaskDao

    companion object{
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase{
            return INSTANCE?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}