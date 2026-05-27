package com.manoj.backgroundvideorecorder.features.schedules.data.repository

import com.manoj.backgroundvideorecorder.core.database.dao.ScheduleDao
import com.manoj.backgroundvideorecorder.features.schedules.data.mapper.toDomain
import com.manoj.backgroundvideorecorder.features.schedules.data.mapper.toEntity
import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import com.manoj.backgroundvideorecorder.features.schedules.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleDao: ScheduleDao
) : ScheduleRepository {

    override fun getAllSchedules(): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getActiveSchedules(): List<Schedule> {
        return scheduleDao.getActiveSchedules().map { it.toDomain() }
    }

    override suspend fun getScheduleById(id: Long): Schedule? {
        return scheduleDao.getScheduleById(id)?.toDomain()
    }

    override suspend fun insertSchedule(schedule: Schedule): Long {
        return scheduleDao.insertSchedule(schedule.toEntity())
    }

    override suspend fun deleteSchedule(schedule: Schedule) {
        scheduleDao.deleteSchedule(schedule.toEntity())
    }
}
