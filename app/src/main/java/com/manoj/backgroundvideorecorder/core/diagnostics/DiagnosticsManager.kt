package com.manoj.backgroundvideorecorder.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class MemoryInfo(
    val totalMemoryBytes: Long,
    val availableMemoryBytes: Long,
    val thresholdBytes: Long,
    val isLowMemory: Boolean,
    val appUsedHeapBytes: Long,
    val appMaxHeapBytes: Long
)

data class BatteryInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float,
    val voltageMv: Int
)

data class StorageInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedPercent: Int
)

data class DiagnosticsState(
    val memory: MemoryInfo,
    val battery: BatteryInfo,
    val storage: StorageInfo,
    val recordingWatchdogAlert: Boolean,
    val lastWatchdogAlertTime: Long
)

@Singleton
class DiagnosticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private var recordingWatchdogAlert: Boolean = false
    private var lastWatchdogAlertTime: Long = 0L

    private val _diagnosticsState = MutableStateFlow(
        DiagnosticsState(
            memory = MemoryInfo(0, 0, 0, false, 0, 0),
            battery = BatteryInfo(0, false, 0f, 0),
            storage = StorageInfo(0, 0, 0),
            recordingWatchdogAlert = false,
            lastWatchdogAlertTime = 0L
        )
    )
    val diagnosticsState: StateFlow<DiagnosticsState> = _diagnosticsState.asStateFlow()

    init {
        updateMetrics()
    }

    fun updateMetrics() {
        _diagnosticsState.value = getLatestDiagnostics()
    }

    fun triggerWatchdogAlert() {
        recordingWatchdogAlert = true
        lastWatchdogAlertTime = System.currentTimeMillis()
        updateMetrics()
        Timber.e("Watchdog alert triggered! Recording appeared frozen or stalled.")
    }

    fun clearWatchdogAlert() {
        recordingWatchdogAlert = false
        updateMetrics()
    }

    fun getLatestDiagnostics(): DiagnosticsState {
        return DiagnosticsState(
            memory = getMemoryInfo(),
            battery = getBatteryInfo(),
            storage = getStorageInfo(),
            recordingWatchdogAlert = recordingWatchdogAlert,
            lastWatchdogAlertTime = lastWatchdogAlertTime
        )
    }

    private fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val appUsedHeap = runtime.totalMemory() - runtime.freeMemory()
        val appMaxHeap = runtime.maxMemory()

        return MemoryInfo(
            totalMemoryBytes = memoryInfo.totalMem,
            availableMemoryBytes = memoryInfo.availMem,
            thresholdBytes = memoryInfo.threshold,
            isLowMemory = memoryInfo.lowMemory,
            appUsedHeapBytes = appUsedHeap,
            appMaxHeapBytes = appMaxHeap
        )
    }

    private fun getBatteryInfo(): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                         status == BatteryManager.BATTERY_STATUS_FULL
                         
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        return BatteryInfo(
            levelPercent = batteryPct,
            isCharging = isCharging,
            temperatureCelsius = temp,
            voltageMv = voltage
        )
    }

    private fun getStorageInfo(): StorageInfo {
        val file = context.filesDir
        val stat = StatFs(file.path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - availableBytes
        val usedPercent = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 0
        
        return StorageInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedPercent = usedPercent
        )
    }
}
