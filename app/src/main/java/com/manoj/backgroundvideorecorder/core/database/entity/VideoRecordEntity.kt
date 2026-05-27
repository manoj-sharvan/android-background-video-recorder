package com.manoj.backgroundvideorecorder.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_records")
data class VideoRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val durationMillis: Long,
    val fileSize: Long,
    val timestamp: Long,
    val isStealth: Boolean
)
