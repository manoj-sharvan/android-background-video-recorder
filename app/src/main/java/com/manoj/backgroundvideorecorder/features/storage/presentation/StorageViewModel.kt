package com.manoj.backgroundvideorecorder.features.storage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manoj.backgroundvideorecorder.core.database.dao.VideoRecordDao
import com.manoj.backgroundvideorecorder.core.database.entity.VideoRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val videoRecordDao: VideoRecordDao
) : ViewModel() {

    private val _state = MutableStateFlow(StorageState())
    val state: StateFlow<StorageState> = _state.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            videoRecordDao.getAllVideoRecords()
                .onStart { _state.update { it.copy(isLoading = true) } }
                .catch { _state.update { it.copy(isLoading = false) } }
                .collect { list ->
                    val sizeSum = list.sumOf { it.fileSize }
                    _state.update { it.copy(isLoading = false, records = list, totalSize = sizeSum) }
                }
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
                isStealth = true
            )
            videoRecordDao.insertVideoRecord(record)
        }
    }

    fun deleteRecord(record: VideoRecordEntity) {
        viewModelScope.launch {
            videoRecordDao.deleteVideoRecord(record)
        }
    }
}
