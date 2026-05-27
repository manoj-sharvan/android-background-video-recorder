package com.manoj.backgroundvideorecorder.background.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.manoj.backgroundvideorecorder.background.service.BackgroundRecordingService
import com.manoj.backgroundvideorecorder.core.common.AppLogger

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        AppLogger.i("ScheduleAlarmReceiver: Received action: $action")

        if (action == ACTION_START_SCHEDULED_RECORDING || action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, BackgroundRecordingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_START_SCHEDULED_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_START_SCHEDULED_RECORDING"
    }
}
