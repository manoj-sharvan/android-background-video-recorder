package com.manoj.backgroundvideorecorder.background.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class RecordingTileService : TileService() {

    @Inject
    lateinit var recordingManager: RecordingManager

    private var scopeJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        AppLogger.i("RecordingTileService: onStartListening")

        scopeJob?.cancel()
        scopeJob = recordingManager.isRecording
            .onEach { isRecording ->
                updateTileState(isRecording)
            }
            .launchIn(serviceScope)
    }

    override fun onStopListening() {
        scopeJob?.cancel()
        scopeJob = null
        AppLogger.i("RecordingTileService: onStopListening")
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val isCurrentlyRecording = recordingManager.isRecording.value
        AppLogger.i("RecordingTileService: onClick, isCurrentlyRecording = $isCurrentlyRecording")

        if (isCurrentlyRecording) {
            val intent = Intent(this, BackgroundRecordingService::class.java).apply {
                action = BackgroundRecordingService.ACTION_STOP_RECORDING
            }
            startService(intent)
        } else {
            val intent = Intent(this, BackgroundRecordingService::class.java).apply {
                action = BackgroundRecordingService.ACTION_START_RECORDING
                putExtra(BackgroundRecordingService.EXTRA_RESOLUTION, "1080p")
                putExtra(BackgroundRecordingService.EXTRA_ENABLE_AUDIO, true)
                putExtra(BackgroundRecordingService.EXTRA_CAMERA_FACING, 0)
                putExtra(BackgroundRecordingService.EXTRA_STEALTH_MODE, false)
                putExtra(BackgroundRecordingService.EXTRA_MAX_DURATION, 10)
                putExtra(BackgroundRecordingService.EXTRA_SPLIT_INTERVAL, 0)
            }
            startService(intent)
        }
    }

    private fun updateTileState(isRecording: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRecording) "Recording..." else "BVR Record"
        tile.updateTile()
    }
}
