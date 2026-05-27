package com.manoj.backgroundvideorecorder.core.di

import android.content.Context
import androidx.room.Room
import com.manoj.backgroundvideorecorder.core.database.AppDatabase
import com.manoj.backgroundvideorecorder.core.database.dao.ScheduleDao
import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "background_video_recorder.db"
        )
        .addMigrations(AppDatabase.MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    @Singleton
    fun provideVideoRecordDao(database: AppDatabase): VideoRecordDao {
        return database.videoRecordDao()
    }

    @Provides
    @Singleton
    fun provideAuditLogDao(database: AppDatabase): AuditLogDao {
        return database.auditLogDao()
    }
}
