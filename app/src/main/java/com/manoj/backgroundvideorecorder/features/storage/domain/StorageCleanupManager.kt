package com.manoj.backgroundvideorecorder.features.storage.domain

import java.io.File

interface StorageCleanupManager {
    suspend fun runRetentionPolicies()
    suspend fun performEmergencyCleanup(requiredSpaceBytes: Long): Boolean
    suspend fun detectAndRepairOrphans()
    fun validateFile(file: File): Boolean
}
