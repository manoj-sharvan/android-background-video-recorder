package com.manoj.backgroundvideorecorder.background.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.manoj.backgroundvideorecorder.R
import com.manoj.backgroundvideorecorder.background.service.BackgroundRecordingService
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.common.StorageMonitor
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecordingWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var recordingManager: RecordingManager

    @Inject
    lateinit var storageMonitor: StorageMonitor

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        AppLogger.i("RecordingWidgetProvider: onUpdate called")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        AppLogger.i("RecordingWidgetProvider: onReceive action = $action")

        if (action == ACTION_WIDGET_UPDATE) {
            val status = intent.getStringExtra(EXTRA_STATUS)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, RecordingWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, status)
            }
        } else if (action == ACTION_WIDGET_TOGGLE) {
            val isCurrentlyRecording = recordingManager.isRecording.value
            AppLogger.i("RecordingWidgetProvider: Toggle button clicked, isCurrentlyRecording = $isCurrentlyRecording")
            if (isCurrentlyRecording) {
                val serviceIntent = Intent(context, BackgroundRecordingService::class.java).apply {
                    this.action = BackgroundRecordingService.ACTION_STOP_RECORDING
                }
                context.startService(serviceIntent)
            } else {
                val serviceIntent = Intent(context, BackgroundRecordingService::class.java).apply {
                    this.action = BackgroundRecordingService.ACTION_START_RECORDING
                    putExtra(BackgroundRecordingService.EXTRA_RESOLUTION, "1080p")
                    putExtra(BackgroundRecordingService.EXTRA_ENABLE_AUDIO, true)
                    putExtra(BackgroundRecordingService.EXTRA_CAMERA_FACING, 0)
                    putExtra(BackgroundRecordingService.EXTRA_STEALTH_MODE, false)
                    putExtra(BackgroundRecordingService.EXTRA_MAX_DURATION, 10)
                    putExtra(BackgroundRecordingService.EXTRA_SPLIT_INTERVAL, 0)
                }
                context.startService(serviceIntent)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        statusText: String?
    ) {
        val views = RemoteViews(context.packageName, R.layout.recording_widget_layout)

        val isRecording = recordingManager.isRecording.value
        val finalStatus = statusText ?: if (isRecording) "BVR: Recording" else "BVR: Idle"
        views.setTextViewText(R.id.widget_status_text, finalStatus)

        val freeBytes = storageMonitor.getFreeSpaceBytes()
        val freeGb = String.format("%.2f GB free", freeBytes.toDouble() / (1024 * 1024 * 1024))
        views.setTextViewText(R.id.widget_storage_text, freeGb)

        val toggleIntent = Intent(context, RecordingWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_toggle_button, pendingIntent)

        views.setTextViewText(R.id.widget_toggle_button, if (isRecording) "STOP" else "REC")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.manoj.backgroundvideorecorder.ACTION_WIDGET_UPDATE"
        const val ACTION_WIDGET_TOGGLE = "com.manoj.backgroundvideorecorder.ACTION_WIDGET_TOGGLE"
        const val EXTRA_STATUS = "extra_status"
    }
}
