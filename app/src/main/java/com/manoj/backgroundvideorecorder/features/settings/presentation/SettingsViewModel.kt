package com.manoj.backgroundvideorecorder.features.settings.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun updateResolution(resolution: String) {
        _state.update { it.copy(videoResolution = resolution) }
    }

    fun updateDuration(minutes: Int) {
        _state.update { it.copy(maxRecordingDurationMinutes = minutes) }
    }

    fun updateStealthMode(enabled: Boolean) {
        _state.update { it.copy(runInStealthModeByDefault = enabled) }
    }

    fun updateNotificationIcon(show: Boolean) {
        _state.update { it.copy(showNotificationIcon = show) }
    }

    fun updateSplitInterval(minutes: Int) {
        _state.update { it.copy(splitVideosIntervalMinutes = minutes) }
    }
}
