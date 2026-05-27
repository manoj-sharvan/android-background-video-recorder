package com.manoj.backgroundvideorecorder.features.storage.presentation

import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity

data class StorageState(
    val records: List<VideoRecordEntity> = emptyList(),
    val totalSize: Long = 0,
    val isLoading: Boolean = false
)
