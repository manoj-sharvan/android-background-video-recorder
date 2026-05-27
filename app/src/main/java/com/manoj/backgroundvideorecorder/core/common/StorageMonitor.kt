package com.manoj.backgroundvideorecorder.core.common

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getFreeSpaceBytes(): Long {
        return try {
            val file = context.filesDir
            val stats = StatFs(file.path)
            stats.availableBytes
        } catch (e: Exception) {
            AppLogger.e(e, "StorageMonitor: Failed to query free partition space")
            0L
        }
    }

    fun isStorageLow(thresholdMb: Int = 100): Boolean {
        val freeBytes = getFreeSpaceBytes()
        val thresholdBytes = thresholdMb * 1024L * 1024L
        return freeBytes < thresholdBytes
    }
}
