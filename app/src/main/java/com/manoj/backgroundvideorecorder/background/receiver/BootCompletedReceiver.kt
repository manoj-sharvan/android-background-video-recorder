package com.manoj.backgroundvideorecorder.background.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.features.schedules.domain.ScheduleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleManager: ScheduleManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            AppLogger.i("BootCompletedReceiver: Device booted. Rescheduling active alarms...")
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scope.launch {
                try {
                    scheduleManager.rescheduleAllActiveAlarms()
                    AppLogger.i("BootCompletedReceiver: Rescheduling complete.")
                } catch (e: Exception) {
                    AppLogger.e(e, "BootCompletedReceiver: Failed to reschedule alarms on boot.")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
