package com.manoj.backgroundvideorecorder.core.di

import android.content.Context
import com.manoj.backgroundvideorecorder.features.security.data.repository.SecurityRepositoryImpl
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import com.manoj.backgroundvideorecorder.features.security.domain.BiometricAuthManager
import com.manoj.backgroundvideorecorder.features.security.domain.RecordingEncryptionManager
import com.manoj.backgroundvideorecorder.features.security.domain.AuditLogger
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideBiometricAuthManager(@ApplicationContext context: Context): BiometricAuthManager {
        return BiometricAuthManager(context)
    }

    @Provides
    @Singleton
    fun provideRecordingEncryptionManager(): RecordingEncryptionManager {
        return RecordingEncryptionManager()
    }

    @Provides
    @Singleton
    fun provideAuditLogger(auditLogDao: AuditLogDao): AuditLogger {
        return AuditLogger(auditLogDao)
    }
}
