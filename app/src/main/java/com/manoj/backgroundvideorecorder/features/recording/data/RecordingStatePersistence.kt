package com.manoj.backgroundvideorecorder.features.recording.data

import android.content.Context
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingStatePersistence @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("recording_state_prefs", Context.MODE_PRIVATE)

    fun saveRecordingState(isActive: Boolean, config: RecordingConfig?) {
        val editor = prefs.edit()
        editor.putBoolean("is_active", isActive)
        if (config != null) {
            editor.putString("session_id", config.sessionId)
            editor.putString("resolution", config.videoResolution)
            editor.putBoolean("enable_audio", config.enableAudio)
            editor.putInt("camera_facing", config.cameraFacing)
            editor.putBoolean("stealth_mode", config.stealthMode)
            editor.putInt("max_duration", config.maxDurationMinutes)
            editor.putInt("split_interval", config.splitIntervalMinutes)
        } else {
            editor.remove("session_id")
        }
        editor.apply()
    }

    fun isRecordingActive(): Boolean = prefs.getBoolean("is_active", false)

    fun getSavedConfig(): RecordingConfig? {
        val sessionId = prefs.getString("session_id", null) ?: return null
        return RecordingConfig(
            sessionId = sessionId,
            videoResolution = prefs.getString("resolution", "1080p") ?: "1080p",
            enableAudio = prefs.getBoolean("enable_audio", true),
            cameraFacing = prefs.getInt("camera_facing", 0),
            stealthMode = prefs.getBoolean("stealth_mode", false),
            maxDurationMinutes = prefs.getInt("max_duration", 10),
            splitIntervalMinutes = prefs.getInt("split_interval", 0)
        )
    }
}
