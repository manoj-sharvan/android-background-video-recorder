package com.manoj.backgroundvideorecorder.features.recording.domain.usecase

import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import com.manoj.backgroundvideorecorder.features.storage.domain.StorageCleanupManager
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import com.manoj.backgroundvideorecorder.features.security.domain.RecordingEncryptionManager
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import java.io.File
import javax.inject.Inject

class SaveVideoRecordUseCase @Inject constructor(
    private val videoRecordDao: VideoRecordDao,
    private val storageCleanupManager: StorageCleanupManager,
    private val securityRepository: SecurityRepository,
    private val encryptionManager: RecordingEncryptionManager
) {
    suspend operator fun invoke(file: File, sizeBytes: Long, durationMillis: Long, isStealth: Boolean) {
        if (!storageCleanupManager.validateFile(file)) {
            AppLogger.e("SaveVideoRecordUseCase: MP4 validation failed. Deleting invalid/corrupt file: ${file.name}")
            if (file.exists()) {
                file.delete()
            }
            return
        }

        val encryptEnabled = securityRepository.isEncryptRecordingsEnabled()
        var finalFilePath = file.absolutePath
        var finalFileName = file.name
        var isEncrypted = false
        var encVersion = 0
        var encTimestamp = 0L

        if (encryptEnabled) {
            try {
                val encFile = File(file.parentFile, "${file.name}.enc")
                encryptionManager.encryptFile(file, encFile)
                if (file.exists()) {
                    file.delete()
                }
                finalFilePath = encFile.absolutePath
                finalFileName = encFile.name
                isEncrypted = true
                encVersion = 1
                encTimestamp = System.currentTimeMillis()
                AppLogger.i("SaveVideoRecordUseCase: Recording encrypted successfully: $finalFileName")
            } catch (e: Exception) {
                AppLogger.e(e, "SaveVideoRecordUseCase: Encryption failed! Saving raw file as fallback.")
            }
        }

        val entity = VideoRecordEntity(
            filePath = finalFilePath,
            fileName = finalFileName,
            durationMillis = durationMillis,
            fileSize = sizeBytes,
            timestamp = System.currentTimeMillis(),
            isStealth = isStealth,
            isProtected = false,
            isEncrypted = isEncrypted,
            encryptionVersion = encVersion,
            encryptionTimestamp = encTimestamp
        )
        videoRecordDao.insertVideoRecord(entity)
    }
}
