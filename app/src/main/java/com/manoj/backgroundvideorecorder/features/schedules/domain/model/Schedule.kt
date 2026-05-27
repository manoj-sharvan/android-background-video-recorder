package com.manoj.backgroundvideorecorder.features.schedules.domain.model

data class Schedule(
    val id: Long = 0,
    val scheduledTimeMillis: Long,
    val durationSeconds: Int,
    val isRecurring: Boolean,
    val intervalMillis: Long = 0,
    val isEnabled: Boolean = true
)
