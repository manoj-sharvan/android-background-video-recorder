package com.manoj.backgroundvideorecorder.core.di

import com.manoj.backgroundvideorecorder.features.recording.data.RecordingManagerImpl
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {

    @Binds
    @Singleton
    abstract fun bindRecordingManager(
        recordingManagerImpl: RecordingManagerImpl
    ): RecordingManager
}
