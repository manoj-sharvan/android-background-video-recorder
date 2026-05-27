package com.manoj.backgroundvideorecorder.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AuditLogEntity>>

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}
