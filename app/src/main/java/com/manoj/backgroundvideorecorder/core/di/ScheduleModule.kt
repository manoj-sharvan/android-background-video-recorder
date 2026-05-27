package com.manoj.backgroundvideorecorder.core.di

import com.manoj.backgroundvideorecorder.features.schedules.data.ScheduleManagerImpl
import com.manoj.backgroundvideorecorder.features.schedules.domain.ScheduleManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleModule {

    @Binds
    @Singleton
    abstract fun bindScheduleManager(
        scheduleManagerImpl: ScheduleManagerImpl
    ): ScheduleManager
}
