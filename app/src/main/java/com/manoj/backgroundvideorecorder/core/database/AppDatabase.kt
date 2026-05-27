package com.manoj.backgroundvideorecorder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.manoj.backgroundvideorecorder.core.database.dao.ScheduleDao
import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.ScheduleEntity
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity

@Database(
    entities = [ScheduleEntity::class, VideoRecordEntity::class, AuditLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun videoRecordDao(): VideoRecordDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `action` TEXT NOT NULL, `detail` TEXT NOT NULL)")
                db.execSQL("ALTER TABLE `video_records` ADD COLUMN `isEncrypted` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `video_records` ADD COLUMN `encryptionVersion` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `video_records` ADD COLUMN `encryptionTimestamp` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
