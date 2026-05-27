package com.manoj.backgroundvideorecorder.features.schedules.domain.usecase

import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import com.manoj.backgroundvideorecorder.features.schedules.domain.repository.ScheduleRepository
import javax.inject.Inject

class AddScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(schedule: Schedule): Long {
        return repository.insertSchedule(schedule)
    }
}
