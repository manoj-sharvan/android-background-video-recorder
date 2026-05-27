package com.manoj.backgroundvideorecorder.features.recording.domain

import java.util.UUID

data class RecordingConfig(
    val sessionId: String = UUID.randomUUID().toString(),
    val videoResolution: String = "1080p",
    val enableAudio: Boolean = true,
    val cameraFacing: Int = 0, // CameraSelector.LENS_FACING_BACK
    val stealthMode: Boolean = false,
    val maxDurationMinutes: Int = 10,
    val splitIntervalMinutes: Int = 0 // 0 means disabled
)
