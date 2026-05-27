package com.manoj.backgroundvideorecorder.features.storage.data

import android.content.Context
import android.media.MediaMetadataRetriever
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import com.manoj.backgroundvideorecorder.features.settings.domain.repository.SettingsRepository
import com.manoj.backgroundvideorecorder.features.storage.domain.StorageCleanupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageCleanupManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRecordDao: VideoRecordDao,
    private val settingsRepository: SettingsRepository
) : StorageCleanupManager {

    private val cleanupMutex = Mutex()

    override suspend fun runRetentionPolicies() {
        cleanupMutex.withLock {
            AppLogger.i("StorageCleanupManager: Starting retention policies check.")
            
            // 1. Age Limit auto-deletion
            runAgeLimitCleanup()

            // 2. Count Limit cleanup
            runCountLimitCleanup()

            // 3. Quota Limit cleanup
            runQuotaLimitCleanup()
        }
    }

    override suspend fun performEmergencyCleanup(requiredSpaceBytes: Long): Boolean {
        cleanupMutex.withLock {
            AppLogger.w("StorageCleanupManager: EMERGENCY LOW STORAGE detected. Reason: EMERGENCY_LOW_STORAGE. Attempting to free $requiredSpaceBytes bytes.")
            
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) return false

            val unprotectedRecords = videoRecordDao.getUnprotectedVideoRecordsAsc()
            var deletedBytes = 0L

            for (record in unprotectedRecords) {
                if (deletedBytes >= requiredSpaceBytes) break

                val file = File(record.filePath)
                val fileSize = if (file.exists()) file.length() else record.fileSize

                // Active file protection: don't delete if modified in last 60 seconds
                if (file.exists() && System.currentTimeMillis() - file.lastModified() < 60000) {
                    continue
                }

                if (file.exists()) {
                    if (file.delete()) {
                        deletedBytes += fileSize
                        AppLogger.i("StorageCleanupManager: [EMERGENCY_LOW_STORAGE] Deleted file: ${file.name}, Size: $fileSize bytes.")
                    }
                }
                videoRecordDao.deleteVideoRecordById(record.id)
            }

            val freeSpace = getFreeSpaceBytes()
            AppLogger.i("StorageCleanupManager: Emergency cleanup finished. Free space now: $freeSpace bytes.")
            return freeSpace >= requiredSpaceBytes
        }
    }

    override suspend fun detectAndRepairOrphans() {
        cleanupMutex.withLock {
            AppLogger.i("StorageCleanupManager: Starting orphan files scan. Reason: ORPHAN_REPAIR.")
            
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) return

            // Get all records in DB
            val dbRecords = videoRecordDao.getAllVideoRecordsList()
            val dbFilePaths = dbRecords.map { it.filePath }.toSet()

            // 1. Files on disk but not in DB
            val filesOnDisk = recordingsDir.listFiles() ?: emptyArray()
            for (file in filesOnDisk) {
                // Ignore if modified in last 60 seconds (could be active recording)
                if (System.currentTimeMillis() - file.lastModified() < 60000) {
                    continue
                }

                if (!dbFilePaths.contains(file.absolutePath)) {
                    if (file.delete()) {
                        AppLogger.w("StorageCleanupManager: [ORPHAN_REPAIR] Deleted unindexed file: ${file.name}")
                    }
                }
            }

            // 2. DB entries pointing to missing files
            for (record in dbRecords) {
                val file = File(record.filePath)
                if (!file.exists()) {
                    videoRecordDao.deleteVideoRecordById(record.id)
                    AppLogger.w("StorageCleanupManager: [ORPHAN_REPAIR] Removed DB record for missing file: ${record.fileName}")
                }
            }
        }
    }

    override fun validateFile(file: File): Boolean {
        if (!file.exists()) {
            AppLogger.e("StorageCleanupManager: File validation failed. File does not exist.")
            return false
        }
        if (file.length() < 1024) {
            AppLogger.e("StorageCleanupManager: File validation failed. File size is too small: ${file.length()} bytes.")
            return false
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            if (duration > 0L) {
                true
            } else {
                AppLogger.e("StorageCleanupManager: File validation failed. Invalid video duration.")
                false
            }
        } catch (e: Exception) {
            AppLogger.e(e, "StorageCleanupManager: File validation failed due to media corruption.")
            false
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }
    }

    private suspend fun runAgeLimitCleanup() {
        val autoDeleteDays = settingsRepository.getAutoDeleteDays()
        if (autoDeleteDays <= 0) return

        val cutoffTimestamp = System.currentTimeMillis() - (autoDeleteDays * 24 * 3600 * 1000L)
        val dbRecords = videoRecordDao.getAllVideoRecordsList()
        val expiredUnprotected = dbRecords.filter { it.timestamp < cutoffTimestamp && !it.isProtected }

        AppLogger.i("StorageCleanupManager: Running [AGE_LIMIT] policy. Auto delete age: $autoDeleteDays days. Expired records found: ${expiredUnprotected.size}")
        
        for (record in expiredUnprotected) {
            val file = File(record.filePath)
            // Skip if modified in last 60 seconds
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < 60000) {
                continue
            }
            if (file.exists()) {
                file.delete()
            }
            videoRecordDao.deleteVideoRecordById(record.id)
            AppLogger.i("StorageCleanupManager: [AGE_LIMIT] Deleted expired recording: ${record.fileName}")
        }
    }

    private suspend fun runCountLimitCleanup() {
        val maxFileCount = settingsRepository.getMaxFileCount()
        if (maxFileCount <= 0) return

        val allRecords = videoRecordDao.getAllVideoRecordsList()
        if (allRecords.size <= maxFileCount) return

        val unprotectedAsc = videoRecordDao.getUnprotectedVideoRecordsAsc()
        var currentCount = allRecords.size
        
        AppLogger.i("StorageCleanupManager: Running [COUNT_LIMIT] policy. Max files allowed: $maxFileCount. Current count: $currentCount")

        for (record in unprotectedAsc) {
            if (currentCount <= maxFileCount) break

            val file = File(record.filePath)
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < 60000) {
                continue
            }
            if (file.exists()) {
                file.delete()
            }
            videoRecordDao.deleteVideoRecordById(record.id)
            currentCount--
            AppLogger.i("StorageCleanupManager: [COUNT_LIMIT] Deleted oldest recording: ${record.fileName}")
        }
    }

    private suspend fun runQuotaLimitCleanup() {
        val maxStorageMb = settingsRepository.getMaxStorageMb()
        if (maxStorageMb <= 0) return

        val quotaBytes = maxStorageMb * 1024L * 1024L
        val allRecords = videoRecordDao.getAllVideoRecordsList()
        var totalBytes = allRecords.sumOf { it.fileSize }

        if (totalBytes <= quotaBytes) return

        val unprotectedAsc = videoRecordDao.getUnprotectedVideoRecordsAsc()
        
        AppLogger.i("StorageCleanupManager: Running [QUOTA_LIMIT] policy. Max storage: $maxStorageMb MB. Current storage: ${totalBytes / (1024 * 1024)} MB")

        for (record in unprotectedAsc) {
            if (totalBytes <= quotaBytes) break

            val file = File(record.filePath)
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < 60000) {
                continue
            }
            val fileSize = if (file.exists()) file.length() else record.fileSize
            if (file.exists()) {
                file.delete()
            }
            videoRecordDao.deleteVideoRecordById(record.id)
            totalBytes -= fileSize
            AppLogger.i("StorageCleanupManager: [QUOTA_LIMIT] Deleted oldest recording: ${record.fileName}, size: $fileSize bytes.")
        }
    }

    private fun getFreeSpaceBytes(): Long {
        return try {
            val stats = android.os.StatFs(context.filesDir.path)
            stats.availableBytes
        } catch (e: Exception) {
            0L
        }
    }
}
