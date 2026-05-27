package com.manoj.backgroundvideorecorder.features.schedules.domain.usecase

import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import com.manoj.backgroundvideorecorder.features.schedules.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSchedulesUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    operator fun invoke(): Flow<List<Schedule>> {
        return repository.getAllSchedules()
    }
}
