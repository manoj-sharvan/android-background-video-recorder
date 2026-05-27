package com.manoj.backgroundvideorecorder.features.storage.presentation

import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity

data class StorageState(
    val records: List<VideoRecordEntity> = emptyList(),
    val totalSize: Long = 0,
    val isLoading: Boolean = false,
    val deviceFreeSpaceBytes: Long = 0L,
    val deviceTotalSpaceBytes: Long = 0L,
    val isCleaning: Boolean = false,
    val searchQuery: String = "",
    val sortBy: String = "date_desc",
    val filterProtected: Boolean = false
)
