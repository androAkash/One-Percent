package com.example.onepercent.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val name : String = "",
    val isPriority : Boolean,
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val isReminderEnabled: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null
)
// Helper extension to get reminder time as LocalTime
fun TaskEntity.getReminderTime(): LocalTime? {
    return if (reminderHour != null && reminderMinute != null) {
        LocalTime.of(reminderHour, reminderMinute)
    } else null
}

// Helper extension to format reminder time
fun TaskEntity.getFormattedReminderTime(): String? {
    return getReminderTime()?.let { time ->
        val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
        val amPm = if (time.hour < 12) "AM" else "PM"
        String.format("%02d:%02d %s", hour, time.minute, amPm)
    }
}