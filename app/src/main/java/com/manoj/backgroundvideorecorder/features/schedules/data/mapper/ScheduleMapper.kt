package com.manoj.backgroundvideorecorder.features.schedules.data.mapper

import com.manoj.backgroundvideorecorder.core.database.entity.ScheduleEntity
import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule

fun ScheduleEntity.toDomain(): Schedule {
    return Schedule(
        id = id,
        scheduledTimeMillis = scheduledTimeMillis,
        durationSeconds = durationSeconds,
        isRecurring = isRecurring,
        intervalMillis = intervalMillis,
        isEnabled = isEnabled,
        repeatType = repeatType,
        daysOfWeek = daysOfWeek,
        lastTriggeredTimeMillis = lastTriggeredTimeMillis,
        lastRunStatus = lastRunStatus,
        lastError = lastError,
        nextTriggerTime = nextTriggerTime
    )
}

fun Schedule.toEntity(): ScheduleEntity {
    return ScheduleEntity(
        id = id,
        scheduledTimeMillis = scheduledTimeMillis,
        durationSeconds = durationSeconds,
        isRecurring = isRecurring,
        intervalMillis = intervalMillis,
        isEnabled = isEnabled,
        repeatType = repeatType,
        daysOfWeek = daysOfWeek,
        lastTriggeredTimeMillis = lastTriggeredTimeMillis,
        lastRunStatus = lastRunStatus,
        lastError = lastError,
        nextTriggerTime = nextTriggerTime
    )
}
