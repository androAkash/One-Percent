package com.example.onepercent.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val name : String = "",
    val isPriority : Boolean,
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val createdDate: Long = System.currentTimeMillis()
)
