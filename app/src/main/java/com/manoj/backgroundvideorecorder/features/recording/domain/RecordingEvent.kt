package com.manoj.backgroundvideorecorder.features.recording.domain

import java.io.File

sealed class RecordingEvent {
    data class Success(val file: File, val sizeBytes: Long, val durationMillis: Long) : RecordingEvent()
    data class Error(val message: String) : RecordingEvent()
}
