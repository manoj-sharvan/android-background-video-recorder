package com.manoj.backgroundvideorecorder.background.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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

    private var fileSegmentCount = 0
    private var tickJob: Job? = null

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
        }

        return START_STICKY
    }

    private fun startRecordingSession(config: RecordingConfig) {
        fileSegmentCount = 0
        val notification = createNotification("Active: 00:00:00 | Segments: 0")
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
        tickJob = lifecycleScope.launch {
            while (true) {
                delay(1000)
                updateForegroundNotification()
                checkStorageLimits()
            }
        }
    }

    private fun checkStorageLimits() {
        if (storageMonitor.isStorageLow(100)) { // 100 MB threshold
            AppLogger.e("BackgroundRecordingService: Low storage detected! Shutting down capturing.")
            statePersistence.saveRecordingState(isActive = false, config = null)
            showLowStorageWarningNotification()
            stopRecordingSession()
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
        val contentText = "Active: $formattedTime | Segments: $fileSegmentCount"
        
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
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
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, BaseApplication.CHANNEL_ID)
            .setContentTitle("Background Recorder Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_STOP_RECORDING"

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
