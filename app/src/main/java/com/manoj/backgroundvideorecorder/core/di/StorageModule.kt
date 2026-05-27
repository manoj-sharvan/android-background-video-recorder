package com.manoj.backgroundvideorecorder.core.di

import com.manoj.backgroundvideorecorder.features.storage.data.StorageCleanupManagerImpl
import com.manoj.backgroundvideorecorder.features.storage.domain.StorageCleanupManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindStorageCleanupManager(
        impl: StorageCleanupManagerImpl
    ): StorageCleanupManager
}
