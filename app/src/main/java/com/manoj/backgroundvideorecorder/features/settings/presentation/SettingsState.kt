package com.manoj.backgroundvideorecorder.features.settings.presentation

data class SettingsState(
    val videoResolution: String = "1080p",
    val maxRecordingDurationMinutes: Int = 10,
    val runInStealthModeByDefault: Boolean = false,
    val showNotificationIcon: Boolean = true,
    val splitVideosIntervalMinutes: Int = 0, // 0 means disabled
    val maxStorageMb: Int = 1024,
    val maxFileCount: Int = 50,
    val autoDeleteDays: Int = 0,
    val emergencyThresholdMb: Int = 100,
    val minimumReservedSpaceMb: Int = 250,
    val isBatteryOptimizationIgnored: Boolean = false,
    val deviceManufacturer: String = "",
    val hasOemBatterySettings: Boolean = false,
    val memoryUsageText: String = "",
    val batteryLevelText: String = "",
    val storageAvailableText: String = "",
    val exportUri: android.net.Uri? = null,
    val isExporting: Boolean = false
)
