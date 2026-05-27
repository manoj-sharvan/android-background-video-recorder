package com.manoj.backgroundvideorecorder.features.recording.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordingManager: RecordingManager,
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _cameraPermissionGranted = MutableStateFlow(false)
    private val _audioPermissionGranted = MutableStateFlow(false)
    private val _isStealthMode = MutableStateFlow(false)
    private val _cameraFacing = MutableStateFlow(LENS_FACING_BACK)
    private val _isHiddenPreview = MutableStateFlow(securityRepository.isHiddenPreviewMode())

    init {
        viewModelScope.launch {
            securityRepository.securityChanges.collect {
                _isHiddenPreview.value = securityRepository.isHiddenPreviewMode()
            }
        }
    }

    val state: StateFlow<RecordingState> = combine(
        recordingManager.isRecording,
        recordingManager.recordingDurationSeconds,
        _isStealthMode,
        _isHiddenPreview,
        _cameraPermissionGranted,
        _audioPermissionGranted,
        _cameraFacing
    ) { flows ->
        val isRecording = flows[0] as Boolean
        val duration = flows[1] as Long
        val stealth = flows[2] as Boolean
        val hiddenPreview = flows[3] as Boolean
        val camera = flows[4] as Boolean
        val audio = flows[5] as Boolean
        val facing = flows[6] as Int

        RecordingState(
            isRecording = isRecording,
            recordingDurationSeconds = duration,
            isStealthMode = stealth,
            isHiddenPreview = hiddenPreview,
            cameraPermissionGranted = camera,
            audioPermissionGranted = audio,
            cameraFacing = facing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecordingState()
    )

    fun toggleStealthMode() {
        _isStealthMode.update { !it }
    }

    fun switchCamera() {
        _cameraFacing.update { facing ->
            if (facing == LENS_FACING_BACK) {
                LENS_FACING_FRONT
            } else {
                LENS_FACING_BACK
            }
        }
    }

    fun updateCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
    }

    fun updateAudioPermission(granted: Boolean) {
        _audioPermissionGranted.value = granted
    }

    companion object {
        private const val LENS_FACING_BACK = 0
        private const val LENS_FACING_FRONT = 1
    }
}
