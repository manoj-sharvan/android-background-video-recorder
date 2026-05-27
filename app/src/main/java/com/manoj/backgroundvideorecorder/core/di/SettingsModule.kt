package com.manoj.backgroundvideorecorder.core.di

import com.manoj.backgroundvideorecorder.features.settings.data.SettingsRepositoryImpl
import com.manoj.backgroundvideorecorder.features.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
