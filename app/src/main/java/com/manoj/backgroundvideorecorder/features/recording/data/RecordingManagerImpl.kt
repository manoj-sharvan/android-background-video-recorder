package com.manoj.backgroundvideorecorder.features.recording.data

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingConfig
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingEvent
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RecordingManager {

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    override val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    override val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    override val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    private val _currentCameraFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    override val currentCameraFacing: StateFlow<Int> = _currentCameraFacing.asStateFlow()

    private val _isStealthMode = MutableStateFlow(false)
    override val isStealthMode: StateFlow<Boolean> = _isStealthMode.asStateFlow()

    private val _recordingEvents = MutableSharedFlow<RecordingEvent>()
    override val recordingEvents: SharedFlow<RecordingEvent> = _recordingEvents.asSharedFlow()

    private var activeRecording: Recording? = null

    private var lastActionTimeMs = 0L
    private val actionCooldownMs = 1000L
    private var videoCapture: VideoCapture<Recorder>? = null

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var recordingStartTime: Long = 0L
    private var currentConfig: RecordingConfig? = null

    // Stability Additions: Mutex, retry counters and hardware callbacks
    private val recordingMutex = Mutex()
    private var retryCount = 0
    private val maxRetries = 3
    private val retryCooldownMs = 10000L // 10 seconds

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            AppLogger.i("RecordingManager: Camera $cameraId is now available.")
        }
        override fun onCameraUnavailable(cameraId: String) {
            AppLogger.w("RecordingManager: Camera $cameraId is now unavailable (in use or disconnected).")
        }
    }

    init {
        try {
            cameraManager.registerAvailabilityCallback(cameraCallback, null)
        } catch (e: Exception) {
            AppLogger.e(e, "RecordingManager: Failed to register camera availability callback")
        }
    }

    override fun setStealthMode(enabled: Boolean) {
        _isStealthMode.value = enabled
    }

    @SuppressLint("MissingPermission")
    override fun startRecording(config: RecordingConfig, lifecycleOwner: LifecycleOwner) {
        scope.launch {
            recordingMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - lastActionTimeMs < actionCooldownMs) {
                    AppLogger.w("RecordingManager: Action ignored due to cooldown.")
                    return@launch
                }
                lastActionTimeMs = now

                if (_isRecording.value) {
                    AppLogger.w("RecordingManager: Start requested, but a session is already active.")
                    return@launch
                }
                _recordingError.value = null
                _isPaused.value = false
                currentConfig = config
                _currentCameraFacing.value = config.cameraFacing
                _isStealthMode.value = config.stealthMode

                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        // Resolution mapping
                        val quality = when (config.videoResolution) {
                            "480p" -> Quality.SD
                            "720p" -> Quality.HD
                            "1080p" -> Quality.FHD
                            else -> Quality.FHD
                        }
                        val qualitySelector = QualitySelector.from(quality)

                        // Create Recorder
                        val recorder = Recorder.Builder()
                            .setExecutor(ContextCompat.getMainExecutor(context))
                            .setQualitySelector(qualitySelector)
                            .build()
                        videoCapture = VideoCapture.withOutput(recorder)

                        // Select Camera
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(config.cameraFacing)
                            .build()

                        // Unbind previous usecases
                        cameraProvider.unbindAll()

                        // Bind to lifecycle
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            videoCapture
                        )

                        // Setup output file
                        val outputDir = File(context.filesDir, "recordings").apply { mkdirs() }
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        val videoFile = File(outputDir, "BVR_${config.sessionId}_$timeStamp.mp4")

                        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

                        // Start recording
                        var pendingRecording = videoCapture?.output?.prepareRecording(context, fileOutputOptions)
                        if (config.enableAudio) {
                            pendingRecording = pendingRecording?.withAudioEnabled()
                        }

                        activeRecording = pendingRecording?.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                            when (recordEvent) {
                                is VideoRecordEvent.Start -> {
                                    _isRecording.value = true
                                    _isPaused.value = false
                                    retryCount = 0 // Reset retries on successful startup
                                    recordingStartTime = System.currentTimeMillis()
                                    startTimer(lifecycleOwner)
                                    AppLogger.i("RecordingManager: Video recording started successfully.")
                                }
                                is VideoRecordEvent.Finalize -> {
                                    _isRecording.value = false
                                    _isPaused.value = false
                                    stopTimer()
                                    if (recordEvent.hasError()) {
                                        val errorMsg = "VideoRecordEvent.Finalize error code: ${recordEvent.error}"
                                        _recordingError.value = errorMsg
                                        AppLogger.e("RecordingManager: $errorMsg")

                                        // Cleanup empty/incomplete file
                                        if (videoFile.exists() && videoFile.length() == 0L) {
                                            videoFile.delete()
                                        }

                                        scope.launch {
                                            _recordingEvents.emit(RecordingEvent.Error(errorMsg))
                                        }

                                        // Handle retry recovery loop
                                        triggerRetryRecovery(config, lifecycleOwner)
                                    } else {
                                        // File integrity validation (File must exist and be > 1KB)
                                        if (videoFile.exists() && videoFile.length() > 1024L) {
                                            AppLogger.i("RecordingManager: Recording validated. File size: ${videoFile.length()} bytes")
                                            val duration = System.currentTimeMillis() - recordingStartTime
                                            scope.launch {
                                                _recordingEvents.emit(
                                                    RecordingEvent.Success(
                                                        file = videoFile,
                                                        sizeBytes = videoFile.length(),
                                                        durationMillis = duration
                                                    )
                                                )
                                            }
                                        } else {
                                            val valError = "File validation failed: File missing or corrupted."
                                            AppLogger.e("RecordingManager: $valError")
                                            if (videoFile.exists()) videoFile.delete()
                                            scope.launch {
                                                _recordingEvents.emit(RecordingEvent.Error(valError))
                                            }
                                        }
                                    }
                                    activeRecording = null
                                }
                            }
                        }

                    } catch (exc: Exception) {
                        val excMsg = exc.message ?: "ProcessCameraProvider config failed"
                        _recordingError.value = excMsg
                        AppLogger.e(exc, "RecordingManager: Camera configuration exception")
                        scope.launch {
                            _recordingEvents.emit(RecordingEvent.Error(excMsg))
                        }
                        triggerRetryRecovery(config, lifecycleOwner)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }

    override fun stopRecording() {
        scope.launch {
            recordingMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - lastActionTimeMs < actionCooldownMs) {
                    AppLogger.w("RecordingManager: Action ignored due to cooldown.")
                    return@launch
                }
                lastActionTimeMs = now

                activeRecording?.stop()
                activeRecording = null
                _isRecording.value = false
                _isPaused.value = false
                stopTimer()
            }
        }
    }

    override fun pauseRecording() {
        scope.launch {
            recordingMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - lastActionTimeMs < actionCooldownMs) {
                    AppLogger.w("RecordingManager: Action ignored due to cooldown.")
                    return@launch
                }
                lastActionTimeMs = now

                if (!_isRecording.value || _isPaused.value) return@launch
                activeRecording?.pause()
                _isPaused.value = true
                AppLogger.i("RecordingManager: Video recording paused.")
            }
        }
    }

    override fun resumeRecording() {
        scope.launch {
            recordingMutex.withLock {
                val now = System.currentTimeMillis()
                if (now - lastActionTimeMs < actionCooldownMs) {
                    AppLogger.w("RecordingManager: Action ignored due to cooldown.")
                    return@launch
                }
                lastActionTimeMs = now

                if (!_isRecording.value || !_isPaused.value) return@launch
                activeRecording?.resume()
                _isPaused.value = false
                AppLogger.i("RecordingManager: Video recording resumed.")
            }
        }
    }

    override fun switchCamera(lifecycleOwner: LifecycleOwner) {
        if (_isRecording.value) return
        val nextFacing = if (_currentCameraFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _currentCameraFacing.value = nextFacing
    }

    private fun triggerRetryRecovery(config: RecordingConfig, lifecycleOwner: LifecycleOwner) {
        if (retryCount < maxRetries) {
            retryCount++
            scope.launch {
                AppLogger.w("RecordingManager: Cooldown recovery active. Retrying ($retryCount/$maxRetries) in 10s...")
                delay(retryCooldownMs)
                startRecording(config, lifecycleOwner)
            }
        } else {
            AppLogger.e("RecordingManager: Max recovery retries reached. Capture canceled.")
        }
    }

    private fun startTimer(lifecycleOwner: LifecycleOwner) {
        timerJob?.cancel()
        _recordingDurationSeconds.value = 0L
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (_isPaused.value) continue
                _recordingDurationSeconds.update { duration ->
                    val nextDuration = duration + 1
                    val config = currentConfig
                    if (config != null && config.splitIntervalMinutes > 0) {
                        val limitSeconds = config.splitIntervalMinutes * 60L
                        if (nextDuration >= limitSeconds) {
                            // Split limit reached!
                            scope.launch(Dispatchers.Main) {
                                AppLogger.i("RecordingManager: Segment limit reached. Splitting file.")
                                stopRecording()
                                delay(500)
                                startRecording(config, lifecycleOwner)
                            }
                        }
                    }
                    nextDuration
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun shutdownGracefully() {
        try {
            activeRecording?.stop()
            activeRecording?.close()
        } catch (e: Exception) {
            // fail-silent
        }
        activeRecording = null
        _isRecording.value = false
        _isPaused.value = false
        stopTimer()
    }
}
