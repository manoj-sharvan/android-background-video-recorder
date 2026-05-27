package com.manoj.backgroundvideorecorder.features.settings.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getMaxStorageMb(): Int
    fun setMaxStorageMb(value: Int)

    fun getMaxFileCount(): Int
    fun setMaxFileCount(value: Int)

    fun getAutoDeleteDays(): Int
    fun setAutoDeleteDays(value: Int)

    fun getEmergencyThresholdMb(): Int
    fun setEmergencyThresholdMb(value: Int)

    fun getMinimumReservedSpaceMb(): Int
    fun setMinimumReservedSpaceMb(value: Int)

    fun getVideoResolution(): String
    fun setVideoResolution(value: String)

    fun getMaxRecordingDurationMinutes(): Int
    fun setMaxRecordingDurationMinutes(value: Int)

    fun getRunInStealthModeByDefault(): Boolean
    fun setRunInStealthModeByDefault(value: Boolean)

    fun getShowNotificationIcon(): Boolean
    fun setShowNotificationIcon(value: Boolean)

    fun getSplitVideosIntervalMinutes(): Int
    fun setSplitVideosIntervalMinutes(value: Int)

    val settingsChanges: Flow<Unit>
}
