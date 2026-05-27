package com.manoj.backgroundvideorecorder.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoRecordDao {
    @Query("SELECT * FROM video_records ORDER BY timestamp DESC")
    fun getAllVideoRecords(): Flow<List<VideoRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoRecord(videoRecord: VideoRecordEntity): Long

    @Delete
    suspend fun deleteVideoRecord(videoRecord: VideoRecordEntity)
}
