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
    val isEnabled: Boolean = true,
    val repeatType: String = "NONE",          // "NONE", "DAILY", "WEEKLY", "WEEKDAYS", "CUSTOM"
    val daysOfWeek: String = "",              // e.g. "2,3,4" (1=Sunday, 2=Monday, etc.)
    val lastTriggeredTimeMillis: Long = 0L,
    val lastRunStatus: String = "PENDING",    // "PENDING", "SUCCESS", "FAILED"
    val lastError: String? = null,
    val nextTriggerTime: Long = 0L
)
