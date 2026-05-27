package com.manoj.backgroundvideorecorder.features.settings.data

import android.content.Context
import com.manoj.backgroundvideorecorder.features.settings.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
    
    // MutableSharedFlow with extra capacity so tryEmit succeeds without suspending
    private val _settingsChanges = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 8)
    override val settingsChanges: Flow<Unit> = _settingsChanges.asSharedFlow()

    private fun notifyChanged() {
        _settingsChanges.tryEmit(Unit)
    }

    override fun getMaxStorageMb(): Int = prefs.getInt("max_storage_mb", 1024)
    override fun setMaxStorageMb(value: Int) {
        prefs.edit().putInt("max_storage_mb", value).apply()
        notifyChanged()
    }

    override fun getMaxFileCount(): Int = prefs.getInt("max_file_count", 50)
    override fun setMaxFileCount(value: Int) {
        prefs.edit().putInt("max_file_count", value).apply()
        notifyChanged()
    }

    override fun getAutoDeleteDays(): Int = prefs.getInt("auto_delete_days", 0)
    override fun setAutoDeleteDays(value: Int) {
        prefs.edit().putInt("auto_delete_days", value).apply()
        notifyChanged()
    }

    override fun getEmergencyThresholdMb(): Int = prefs.getInt("emergency_threshold_mb", 100)
    override fun setEmergencyThresholdMb(value: Int) {
        prefs.edit().putInt("emergency_threshold_mb", value).apply()
        notifyChanged()
    }

    override fun getMinimumReservedSpaceMb(): Int = prefs.getInt("minimum_reserved_space_mb", 250)
    override fun setMinimumReservedSpaceMb(value: Int) {
        prefs.edit().putInt("minimum_reserved_space_mb", value).apply()
        notifyChanged()
    }

    override fun getVideoResolution(): String = prefs.getString("video_resolution", "1080p") ?: "1080p"
    override fun setVideoResolution(value: String) {
        prefs.edit().putString("video_resolution", value).apply()
        notifyChanged()
    }

    override fun getMaxRecordingDurationMinutes(): Int = prefs.getInt("max_recording_duration_minutes", 10)
    override fun setMaxRecordingDurationMinutes(value: Int) {
        prefs.edit().putInt("max_recording_duration_minutes", value).apply()
        notifyChanged()
    }

    override fun getRunInStealthModeByDefault(): Boolean = prefs.getBoolean("run_in_stealth_mode_default", false)
    override fun setRunInStealthModeByDefault(value: Boolean) {
        prefs.edit().putBoolean("run_in_stealth_mode_default", value).apply()
        notifyChanged()
    }

    override fun getShowNotificationIcon(): Boolean = prefs.getBoolean("show_notification_icon", true)
    override fun setShowNotificationIcon(value: Boolean) {
        prefs.edit().putBoolean("show_notification_icon", value).apply()
        notifyChanged()
    }

    override fun getSplitVideosIntervalMinutes(): Int = prefs.getInt("split_videos_interval_minutes", 0)
    override fun setSplitVideosIntervalMinutes(value: Int) {
        prefs.edit().putInt("split_videos_interval_minutes", value).apply()
        notifyChanged()
    }
}
