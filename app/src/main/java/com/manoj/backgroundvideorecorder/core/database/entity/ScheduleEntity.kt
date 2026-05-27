package com.manoj.backgroundvideorecorder.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduledTimeMillis: Long,
    val durationSeconds: Int,
    val isRecurring: Boolean,
    val intervalMillis: Long = 0,
    val isEnabled: Boolean = true
)
