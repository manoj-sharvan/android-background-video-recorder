package com.manoj.backgroundvideorecorder.features.recording.domain.usecase

import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import java.io.File
import javax.inject.Inject

class SaveVideoRecordUseCase @Inject constructor(
    private val videoRecordDao: VideoRecordDao
) {
    suspend operator fun invoke(file: File, sizeBytes: Long, durationMillis: Long, isStealth: Boolean) {
        val entity = VideoRecordEntity(
            filePath = file.absolutePath,
            fileName = file.name,
            durationMillis = durationMillis,
            fileSize = sizeBytes,
            timestamp = System.currentTimeMillis(),
            isStealth = isStealth
        )
        videoRecordDao.insertVideoRecord(entity)
    }
}
