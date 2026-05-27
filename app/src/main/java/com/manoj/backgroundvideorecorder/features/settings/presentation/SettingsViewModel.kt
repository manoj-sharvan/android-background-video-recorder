package com.manoj.backgroundvideorecorder.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manoj.backgroundvideorecorder.features.settings.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val batteryHelper: com.manoj.backgroundvideorecorder.core.common.BatteryOptimizationHelper,
    private val diagnosticsManager: com.manoj.backgroundvideorecorder.core.diagnostics.DiagnosticsManager,
    private val logExporter: com.manoj.backgroundvideorecorder.core.diagnostics.LogExporter,
    val auditLogDao: com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
        viewModelScope.launch {
            settingsRepository.settingsChanges.collect {
                loadSettings()
            }
        }
    }

    fun loadSettings() {
        diagnosticsManager.updateMetrics()
        val stats = diagnosticsManager.getLatestDiagnostics()
        
        val ramUsedMb = stats.memory.appUsedHeapBytes / 1024 / 1024
        val ramMaxMb = stats.memory.appMaxHeapBytes / 1024 / 1024
        val freeStorageMb = stats.storage.availableBytes / 1024 / 1024

        _state.update {
            it.copy(
                videoResolution = settingsRepository.getVideoResolution(),
                maxRecordingDurationMinutes = settingsRepository.getMaxRecordingDurationMinutes(),
                runInStealthModeByDefault = settingsRepository.getRunInStealthModeByDefault(),
                showNotificationIcon = settingsRepository.getShowNotificationIcon(),
                splitVideosIntervalMinutes = settingsRepository.getSplitVideosIntervalMinutes(),
                maxStorageMb = settingsRepository.getMaxStorageMb(),
                maxFileCount = settingsRepository.getMaxFileCount(),
                autoDeleteDays = settingsRepository.getAutoDeleteDays(),
                emergencyThresholdMb = settingsRepository.getEmergencyThresholdMb(),
                minimumReservedSpaceMb = settingsRepository.getMinimumReservedSpaceMb(),
                isBatteryOptimizationIgnored = batteryHelper.isIgnoringBatteryOptimizations(),
                deviceManufacturer = batteryHelper.getDeviceManufacturer(),
                hasOemBatterySettings = batteryHelper.getOemBatterySettingsIntent() != null,
                memoryUsageText = "$ramUsedMb MB / $ramMaxMb MB Used",
                batteryLevelText = "${stats.battery.levelPercent}% (${if (stats.battery.isCharging) "Charging" else "Discharging"}, ${stats.battery.temperatureCelsius}°C)",
                storageAvailableText = "$freeStorageMb MB Free"
            )
        }
    }

    fun updateResolution(resolution: String) {
        settingsRepository.setVideoResolution(resolution)
    }

    fun updateDuration(minutes: Int) {
        settingsRepository.setMaxRecordingDurationMinutes(minutes)
    }

    fun updateStealthMode(enabled: Boolean) {
        settingsRepository.setRunInStealthModeByDefault(enabled)
    }

    fun updateNotificationIcon(show: Boolean) {
        settingsRepository.setShowNotificationIcon(show)
    }

    fun updateSplitInterval(minutes: Int) {
        settingsRepository.setSplitVideosIntervalMinutes(minutes)
    }

    fun updateMaxStorageMb(mb: Int) {
        settingsRepository.setMaxStorageMb(mb)
    }

    fun updateMaxFileCount(count: Int) {
        settingsRepository.setMaxFileCount(count)
    }

    fun updateAutoDeleteDays(days: Int) {
        settingsRepository.setAutoDeleteDays(days)
    }

    fun updateEmergencyThresholdMb(mb: Int) {
        settingsRepository.setEmergencyThresholdMb(mb)
    }

    fun updateMinimumReservedSpaceMb(mb: Int) {
        settingsRepository.setMinimumReservedSpaceMb(mb)
    }

    fun requestBatteryWhitelistIntent(): android.content.Intent {
        return batteryHelper.getBatteryOptimizationIntent()
    }

    fun requestOemBatterySettingsIntent(): android.content.Intent? {
        return batteryHelper.getOemBatterySettingsIntent()
    }

    fun exportDiagnosticLogs(onCompleted: (android.net.Uri?) -> Unit) {
        if (_state.value.isExporting) return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val uri = logExporter.exportLogs()
            _state.update { it.copy(isExporting = false, exportUri = uri) }
            onCompleted(uri)
        }
    }

    fun clearExportUri() {
        _state.update { it.copy(exportUri = null) }
    }
}
