package com.manoj.backgroundvideorecorder.features.schedules.domain.model

data class Schedule(
    val id: Long = 0,
    val scheduledTimeMillis: Long,
    val durationSeconds: Int,
    val isRecurring: Boolean,
    val intervalMillis: Long = 0,
    val isEnabled: Boolean = true,
    val repeatType: String = "NONE",
    val daysOfWeek: String = "",
    val lastTriggeredTimeMillis: Long = 0L,
    val lastRunStatus: String = "PENDING",
    val lastError: String? = null,
    val nextTriggerTime: Long = 0L
)
