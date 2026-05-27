package com.manoj.backgroundvideorecorder.features.schedules.domain.repository

import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun getAllSchedules(): Flow<List<Schedule>>
    suspend fun getActiveSchedules(): List<Schedule>
    suspend fun getScheduleById(id: Long): Schedule?
    suspend fun insertSchedule(schedule: Schedule): Long
    suspend fun deleteSchedule(schedule: Schedule)
}
