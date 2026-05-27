package com.manoj.backgroundvideorecorder.core.di

import com.manoj.backgroundvideorecorder.features.schedules.data.repository.ScheduleRepositoryImpl
import com.manoj.backgroundvideorecorder.features.schedules.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        scheduleRepositoryImpl: ScheduleRepositoryImpl
    ): ScheduleRepository
}
