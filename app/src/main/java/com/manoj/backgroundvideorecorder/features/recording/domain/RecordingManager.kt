package com.manoj.backgroundvideorecorder.features.recording.domain

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RecordingManager {
    val isRecording: StateFlow<Boolean>
    val recordingDurationSeconds: StateFlow<Long>
    val recordingError: StateFlow<String?>
    val currentCameraFacing: StateFlow<Int>
    val isStealthMode: StateFlow<Boolean>
    val recordingEvents: SharedFlow<RecordingEvent>

    fun startRecording(config: RecordingConfig, lifecycleOwner: LifecycleOwner)
    fun stopRecording()
    fun switchCamera(lifecycleOwner: LifecycleOwner)
    fun setStealthMode(enabled: Boolean)
}
