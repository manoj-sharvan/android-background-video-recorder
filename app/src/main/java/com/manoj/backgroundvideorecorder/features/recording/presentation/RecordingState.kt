package com.manoj.backgroundvideorecorder.features.recording.presentation

data class RecordingState(
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Long = 0,
    val isStealthMode: Boolean = false,
    val isHiddenPreview: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val audioPermissionGranted: Boolean = false,
    val cameraFacing: Int = 0 // 0 = BACK, 1 = FRONT
)
