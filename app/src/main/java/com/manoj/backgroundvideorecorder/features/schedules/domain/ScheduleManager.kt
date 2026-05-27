package com.manoj.backgroundvideorecorder.features.schedules.domain

import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule

interface ScheduleManager {
    fun scheduleAlarm(schedule: Schedule)
    fun cancelAlarm(schedule: Schedule)
    fun rescheduleAllActiveAlarms()
}
