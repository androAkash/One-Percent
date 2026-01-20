package com.example.onepercent.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "priority_task")
data class TaskCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int=0,
    val taskName:String,
    val taskId: Int,
    val completedDate:Long
)
