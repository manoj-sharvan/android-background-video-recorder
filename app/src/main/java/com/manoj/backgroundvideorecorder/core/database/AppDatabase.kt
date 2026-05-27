package com.manoj.backgroundvideorecorder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.manoj.backgroundvideorecorder.core.database.dao.ScheduleDao
import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.entity.ScheduleEntity
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity

@Database(
    entities = [ScheduleEntity::class, VideoRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun videoRecordDao(): VideoRecordDao
}
