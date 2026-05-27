package com.manoj.backgroundvideorecorder.features.settings.presentation

data class SettingsState(
    val videoResolution: String = "1080p",
    val maxRecordingDurationMinutes: Int = 10,
    val runInStealthModeByDefault: Boolean = false,
    val showNotificationIcon: Boolean = true,
    val splitVideosIntervalMinutes: Int = 0 // 0 means disabled
)
