package com.manoj.backgroundvideorecorder.features.storage.presentation

import android.content.Context
import android.content.Intent
import android.os.StatFs
import java.io.File
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import com.manoj.backgroundvideorecorder.features.storage.domain.StorageCleanupManager
import com.manoj.backgroundvideorecorder.features.security.domain.RecordingEncryptionManager
import com.manoj.backgroundvideorecorder.features.security.domain.AuditLogger
import com.manoj.backgroundvideorecorder.features.security.domain.AuditAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRecordDao: VideoRecordDao,
    private val storageCleanupManager: StorageCleanupManager,
    private val encryptionManager: RecordingEncryptionManager,
    private val auditLogger: AuditLogger
) : ViewModel() {

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state.asStateFlow()

    private var allRecords: List<VideoRecordEntity> = emptyList()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            videoRecordDao.getAllVideoRecords()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { _state.update { it.copy(isLoading = false) } }
                .collect { list ->
                    allRecords = list
                    val sizeSum = list.sumOf { it.fileSize }

                    // Retrieve disk stats
                    val path = context.filesDir.path
                    val stats = StatFs(path)
                    val freeSpace = stats.availableBytes
                    val totalSpace = stats.totalBytes

                    _state.update {
                        it.copy(
                            isLoading = false,
                            totalSize = sizeSum,
                            deviceFreeSpaceBytes = freeSpace,
                            deviceTotalSpaceBytes = totalSpace
                        )
                    }
                    updateFilteredRecords()
                }
        }
    }

    private fun updateFilteredRecords() {
        val query = _state.value.searchQuery
        val sort = _state.value.sortBy
        val filterProt = _state.value.filterProtected

        var filtered = allRecords.filter { record ->
            record.fileName.contains(query, ignoreCase = true)
        }

        if (filterProt) {
            filtered = filtered.filter { it.isProtected }
        }

        val sorted = when (sort) {
            "date_asc" -> filtered.sortedBy { it.timestamp }
            "date_desc" -> filtered.sortedByDescending { it.timestamp }
            "size_asc" -> filtered.sortedBy { it.fileSize }
            "size_desc" -> filtered.sortedByDescending { it.fileSize }
            "duration_desc" -> filtered.sortedByDescending { it.durationMillis }
            else -> filtered.sortedByDescending { it.timestamp }
        }

        _state.update { it.copy(records = sorted) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        updateFilteredRecords()
    }

    fun setSortBy(sortBy: String) {
        _state.update { it.copy(sortBy = sortBy) }
        updateFilteredRecords()
    }

    fun setFilterProtected(filterProtected: Boolean) {
        _state.update { it.copy(filterProtected = filterProtected) }
        updateFilteredRecords()
    }

    fun getShareIntent(record: VideoRecordEntity): Intent? {
        val file = File(record.filePath)
        if (!file.exists()) return null

        val shareFile: File
        if (record.isEncrypted) {
            try {
                val exportDir = File(context.cacheDir, "decrypted_exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                shareFile = File(exportDir, record.fileName.removeSuffix(".enc"))
                encryptionManager.decryptFile(file, shareFile)

                auditLogger.log(AuditAction.EXPORT_RECORDING, "Exported encrypted file: ${record.fileName}")

                // Auto-delete temp decrypted file after 5 minutes
                viewModelScope.launch(Dispatchers.IO) {
                    delay(300_000)
                    if (shareFile.exists()) {
                        shareFile.delete()
                    }
                }
            } catch (e: Exception) {
                return null
            }
        } else {
            shareFile = file
            auditLogger.log(AuditAction.EXPORT_RECORDING, "Shared raw file: ${record.fileName}")
        }

        val authority = "com.manoj.backgroundvideorecorder.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, shareFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isCleaning = true) }
            storageCleanupManager.runRetentionPolicies()
            _state.update { it.copy(isCleaning = false) }
        }
    }

    fun addDemoRecord(filePath: String, size: Long, duration: Long) {
        viewModelScope.launch {
            val record = VideoRecordEntity(
                filePath = filePath,
                fileName = filePath.substringAfterLast('/'),
                durationMillis = duration,
                fileSize = size,
                timestamp = System.currentTimeMillis(),
                isStealth = true,
                isProtected = false
            )
            videoRecordDao.insertVideoRecord(record)
        }
    }

    fun deleteRecord(record: VideoRecordEntity) {
        viewModelScope.launch {
            val file = File(record.filePath)
            if (file.exists()) {
                file.delete()
            }
            videoRecordDao.deleteVideoRecord(record)
            auditLogger.log(AuditAction.DELETE_RECORDING, "Deleted file: ${record.fileName}")
        }
    }

    fun toggleProtection(record: VideoRecordEntity) {
        viewModelScope.launch {
            videoRecordDao.updateProtectionStatus(record.id, !record.isProtected)
        }
    }

    fun runManualOrphanCleanup() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isCleaning = true) }
            storageCleanupManager.detectAndRepairOrphans()
            _state.update { it.copy(isCleaning = false) }
        }
    }
}
