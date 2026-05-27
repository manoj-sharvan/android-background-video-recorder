package com.manoj.backgroundvideorecorder.background.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.util.Locale
import java.io.File
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.manoj.backgroundvideorecorder.BaseApplication
import com.manoj.backgroundvideorecorder.MainActivity
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.common.StorageMonitor
import com.manoj.backgroundvideorecorder.features.recording.data.RecordingStatePersistence
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingConfig
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingEvent
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import com.manoj.backgroundvideorecorder.features.recording.domain.usecase.SaveVideoRecordUseCase
import com.manoj.backgroundvideorecorder.features.settings.domain.repository.SettingsRepository
import com.manoj.backgroundvideorecorder.features.storage.domain.StorageCleanupManager
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundRecordingService : Service(), LifecycleOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    @Inject
    lateinit var recordingManager: RecordingManager

    @Inject
    lateinit var saveVideoRecordUseCase: SaveVideoRecordUseCase

    @Inject
    lateinit var storageMonitor: StorageMonitor

    @Inject
    lateinit var statePersistence: RecordingStatePersistence

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var storageCleanupManager: StorageCleanupManager

    @Inject
    lateinit var securityRepository: SecurityRepository

    @Inject
    lateinit var diagnosticsManager: com.manoj.backgroundvideorecorder.core.diagnostics.DiagnosticsManager

    private var fileSegmentCount = 0
    private var tickJob: Job? = null
    private var lastRecordedDurationSeconds = -1L
    private var lastDurationChangeTimeMs = 0L

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        AppLogger.i("BackgroundRecordingService: onCreate")

        // Observe recording events and persist logs when recordings complete
        lifecycleScope.launch {
            recordingManager.recordingEvents.collect { event ->
                when (event) {
                    is RecordingEvent.Success -> {
                        fileSegmentCount++
                        AppLogger.i("BackgroundRecordingService: Saving recording metadata to Room DB: ${event.file.name}")
                        saveVideoRecordUseCase(
                            file = event.file,
                            sizeBytes = event.sizeBytes,
                            durationMillis = event.durationMillis,
                            isStealth = recordingManager.isStealthMode.value
                        )
                        lifecycleScope.launch {
                            storageCleanupManager.runRetentionPolicies()
                        }
                    }
                    is RecordingEvent.Error -> {
                        AppLogger.e("BackgroundRecordingService: Recording error: ${event.message}")
                        // Stop updating status
                        tickJob?.cancel()
                        stopSelf()
                    }
                }
            }
        }

        // Instantly update notification actions when pause status transitions
        lifecycleScope.launch {
            recordingManager.isPaused.collect {
                if (recordingManager.isRecording.value) {
                    updateForegroundNotification()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        AppLogger.i("BackgroundRecordingService: onStartCommand with action: $action")

        if (intent == null) {
            // Process death recovery: Service restarted by the system
            AppLogger.w("BackgroundRecordingService: Intent is null. Restoring state after process restart.")
            if (statePersistence.isRecordingActive()) {
                val savedConfig = statePersistence.getSavedConfig()
                if (savedConfig != null) {
                    AppLogger.i("BackgroundRecordingService: Restoring recording for config: $savedConfig")
                    startRecordingSession(savedConfig)
                } else {
                    stopSelf()
                }
            } else {
                stopSelf()
            }
            return START_STICKY
        }

        when (action) {
            ACTION_START_RECORDING -> {
                val resolution = intent.getStringExtra(EXTRA_RESOLUTION) ?: "1080p"
                val enableAudio = intent.getBooleanExtra(EXTRA_ENABLE_AUDIO, true)
                val cameraFacing = intent.getIntExtra(EXTRA_CAMERA_FACING, 0)
                val stealthMode = intent.getBooleanExtra(EXTRA_STEALTH_MODE, false)
                val maxDuration = intent.getIntExtra(EXTRA_MAX_DURATION, 10)
                val splitInterval = intent.getIntExtra(EXTRA_SPLIT_INTERVAL, 0)

                val config = RecordingConfig(
                    videoResolution = resolution,
                    enableAudio = enableAudio,
                    cameraFacing = cameraFacing,
                    stealthMode = stealthMode,
                    maxDurationMinutes = maxDuration,
                    splitIntervalMinutes = splitInterval
                )

                // Save recording configuration to persist state
                statePersistence.saveRecordingState(isActive = true, config = config)
                startRecordingSession(config)
            }
            ACTION_STOP_RECORDING -> {
                AppLogger.i("BackgroundRecordingService: Stopping CameraX capture.")
                statePersistence.saveRecordingState(isActive = false, config = null)
                stopRecordingSession()
            }
            ACTION_PAUSE_RECORDING -> {
                AppLogger.i("BackgroundRecordingService: Pausing CameraX capture.")
                recordingManager.pauseRecording()
            }
            ACTION_RESUME_RECORDING -> {
                AppLogger.i("BackgroundRecordingService: Resuming CameraX capture.")
                recordingManager.resumeRecording()
            }
        }

        return START_STICKY
    }

    private fun startRecordingSession(config: RecordingConfig) {
        fileSegmentCount = 0
        val notification = createNotification("Recording: 00:00:00 | Segments: 0 | Storage Used: 0.00 Bytes")
        startForeground(NOTIFICATION_ID, notification)

        recordingManager.startRecording(config, this)
        startNotificationTicks()
    }

    private fun stopRecordingSession() {
        tickJob?.cancel()
        recordingManager.stopRecording()
        stopSelf()
    }

    private fun startNotificationTicks() {
        tickJob?.cancel()
        lastDurationChangeTimeMs = System.currentTimeMillis()
        tickJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                updateForegroundNotification()
                checkStorageLimits()
                
                // Update system diagnostics
                diagnosticsManager.updateMetrics()
                val diag = diagnosticsManager.getLatestDiagnostics()
                
                // Memory warning / recovery
                if (diag.memory.isLowMemory) {
                    AppLogger.w("BackgroundRecordingService: Low memory condition detected! Active RAM available: ${diag.memory.availableMemoryBytes / 1024 / 1024} MB. Requesting Garbage Collection.")
                    System.gc()
                }

                // Recording watchdog Stall/Frozen checks
                val currentDuration = recordingManager.recordingDurationSeconds.value
                val now = System.currentTimeMillis()
                if (recordingManager.isRecording.value && !recordingManager.isPaused.value) {
                    if (currentDuration != lastRecordedDurationSeconds) {
                        lastRecordedDurationSeconds = currentDuration
                        lastDurationChangeTimeMs = now
                        diagnosticsManager.clearWatchdogAlert()
                    } else if (now - lastDurationChangeTimeMs > 10000L) { // 10 seconds stalled
                        AppLogger.w("BackgroundRecordingService: Watchdog detected stalled recording duration!")
                        diagnosticsManager.triggerWatchdogAlert()
                        lastDurationChangeTimeMs = now // Reset timer to avoid loop triggers
                        lifecycleScope.launch {
                            AppLogger.w("BackgroundRecordingService: Running watchdog recovery: restarting session.")
                            val savedConfig = statePersistence.getSavedConfig()
                            stopRecordingSession()
                            delay(2000)
                            if (savedConfig != null) {
                                startRecordingSession(savedConfig)
                            }
                        }
                    }
                } else {
                    lastRecordedDurationSeconds = -1L
                    lastDurationChangeTimeMs = now
                }
            }
        }
    }

    private fun checkStorageLimits() {
        val emergencyThreshold = settingsRepository.getEmergencyThresholdMb()
        val reservedSpace = settingsRepository.getMinimumReservedSpaceMb()

        if (storageMonitor.isStorageLow(emergencyThreshold + reservedSpace)) {
            AppLogger.w("BackgroundRecordingService: Storage low. Running emergency cleanup.")
            lifecycleScope.launch {
                val freed = storageCleanupManager.performEmergencyCleanup(500 * 1024L * 1024L)
                if (!freed && storageMonitor.isStorageLow(emergencyThreshold)) {
                    AppLogger.e("BackgroundRecordingService: Storage critically low! Gracefully halting recording session.")
                    statePersistence.saveRecordingState(isActive = false, config = null)
                    showLowStorageWarningNotification()
                    stopRecordingSession()
                }
            }
        }
    }

    private fun updateForegroundNotification() {
        val durationSeconds = recordingManager.recordingDurationSeconds.value
        val formattedTime = String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            durationSeconds / 3600,
            (durationSeconds % 3600) / 60,
            durationSeconds % 60
        )
        val isPaused = recordingManager.isPaused.value
        val statusText = if (isPaused) " (Paused)" else ""
        val storageText = formatBytes(getRecordingsSize())
        val contentText = "Recording: $formattedTime$statusText | Segments: $fileSegmentCount | Storage Used: $storageText"

        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        updateAllWidgets(contentText)
    }

    private fun updateAllWidgets(contentText: String) {
        val widgetIntent = Intent(this, com.manoj.backgroundvideorecorder.background.receiver.RecordingWidgetProvider::class.java).apply {
            action = "com.manoj.backgroundvideorecorder.ACTION_WIDGET_UPDATE"
            putExtra("extra_status", contentText)
        }
        sendBroadcast(widgetIntent)
    }

    private fun getRecordingsSize(): Long {
        val dir = File(filesDir, "recordings")
        if (!dir.exists()) return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0.00 Bytes"
        val k = 1024.0
        val sizes = arrayOf("Bytes", "KB", "MB", "GB")
        val i = kotlin.math.floor(kotlin.math.log(bytes.toDouble(), k)).toInt()
        return String.format(Locale.getDefault(), "%.2f %s", bytes / java.lang.Math.pow(k, i.toDouble()), sizes[i])
    }

    private fun showLowStorageWarningNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val warningNotification = NotificationCompat.Builder(this, BaseApplication.CHANNEL_ID)
            .setContentTitle("BVR: Recording Halted")
            .setContentText("Recording stopped automatically due to low device storage.")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(WARNING_NOTIFICATION_ID, warningNotification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return null
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        AppLogger.i("BackgroundRecordingService: onDestroy")
        tickJob?.cancel()
        recordingManager.stopRecording()
        super.onDestroy()
    }

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    private fun createNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isPrivate = securityRepository.isPrivateNotificationMode()
        val builder = NotificationCompat.Builder(this, BaseApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)

        if (isPrivate) {
            builder.setContentTitle("Service Running")
            builder.setContentText("System service active")
        } else {
            val isPaused = recordingManager.isPaused.value
            val pauseResumeIntent = Intent(this, BackgroundRecordingService::class.java).apply {
                action = if (isPaused) ACTION_RESUME_RECORDING else ACTION_PAUSE_RECORDING
            }
            val pauseResumePendingIntent = PendingIntent.getService(
                this, 1, pauseResumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val pauseResumeLabel = if (isPaused) "Resume" else "Pause"

            val stopIntent = Intent(this, BackgroundRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            val stopPendingIntent = PendingIntent.getService(
                this, 2, stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            builder.setContentTitle("BVR Background Capture")
            builder.setContentText(contentText)
            builder.addAction(android.R.drawable.ic_media_play, pauseResumeLabel, pauseResumePendingIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
        }

        return builder.build()
    }

    companion object {
        const val ACTION_START_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_RESUME_RECORDING"

        const val EXTRA_RESOLUTION = "extra_resolution"
        const val EXTRA_ENABLE_AUDIO = "extra_enable_audio"
        const val EXTRA_CAMERA_FACING = "extra_camera_facing"
        const val EXTRA_STEALTH_MODE = "extra_stealth_mode"
        const val EXTRA_MAX_DURATION = "extra_max_duration"
        const val EXTRA_SPLIT_INTERVAL = "extra_split_interval"

        private const val NOTIFICATION_ID = 101
        private const val WARNING_NOTIFICATION_ID = 102
    }
}

