package com.manoj.backgroundvideorecorder.features.schedules.presentation

import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule

data class SchedulesState(
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val exactAlarmPermissionGranted: Boolean = false
)
