package com.manoj.backgroundvideorecorder.background.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.manoj.backgroundvideorecorder.background.service.BackgroundRecordingService
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.ScheduleDao
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import com.manoj.backgroundvideorecorder.features.schedules.data.mapper.toDomain
import com.manoj.backgroundvideorecorder.features.schedules.domain.ScheduleManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduleManager: ScheduleManager

    @Inject
    lateinit var scheduleDao: ScheduleDao

    @Inject
    lateinit var recordingManager: RecordingManager

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        AppLogger.i("ScheduleAlarmReceiver: Received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            AppLogger.i("ScheduleAlarmReceiver: Restoring all alarms post-reboot")
            scheduleManager.rescheduleAllActiveAlarms()
            return
        }

        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId == -1L) return

        when (action) {
            ACTION_START_SCHEDULED_RECORDING -> {
                val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, 10)
                AppLogger.i("ScheduleAlarmReceiver: Triggering scheduled recording. ID: $scheduleId, Duration: $duration s")

                // Conflict protection: check if already recording
                if (recordingManager.isRecording.value) {
                    AppLogger.w("ScheduleAlarmReceiver: Recording session is already active. Conflicting schedule $scheduleId is skipped.")
                    updateScheduleStatus(scheduleId, "FAILED", "Overlapping recording session detected.")
                    cancelStopAlarm(context, scheduleId)
                    rescheduleNextOccurrence(scheduleId)
                    return
                }

                // Start Foreground Service with extra duration settings
                val serviceIntent = Intent(context, BackgroundRecordingService::class.java).apply {
                    this.action = BackgroundRecordingService.ACTION_START_RECORDING
                    putExtra(BackgroundRecordingService.EXTRA_MAX_DURATION, duration / 60) // convert to min
                    putExtra(BackgroundRecordingService.EXTRA_RESOLUTION, "1080p")
                    putExtra(BackgroundRecordingService.EXTRA_ENABLE_AUDIO, true)
                }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    updateScheduleStatus(scheduleId, "SUCCESS", null)
                } catch (e: Exception) {
                    AppLogger.e("ScheduleAlarmReceiver: Failed to start recording service", e)
                    updateScheduleStatus(scheduleId, "FAILED", e.message ?: "Failed to start service")
                    cancelStopAlarm(context, scheduleId)
                    rescheduleNextOccurrence(scheduleId)
                }
            }
            ACTION_STOP_SCHEDULED_RECORDING -> {
                AppLogger.i("ScheduleAlarmReceiver: Triggering scheduled recording stop. ID: $scheduleId")
                val serviceIntent = Intent(context, BackgroundRecordingService::class.java).apply {
                    this.action = BackgroundRecordingService.ACTION_STOP_RECORDING
                }
                context.startService(serviceIntent)
                rescheduleNextOccurrence(scheduleId)
                disableOneTimeSchedule(scheduleId)
            }
        }
    }

    private fun updateScheduleStatus(scheduleId: Long, status: String, error: String?) {
        scope.launch {
            val entity = scheduleDao.getScheduleById(scheduleId)
            if (entity != null) {
                val updated = entity.copy(
                    lastTriggeredTimeMillis = System.currentTimeMillis(),
                    lastRunStatus = status,
                    lastError = error
                )
                scheduleDao.updateSchedule(updated)
            }
        }
    }

    private fun rescheduleNextOccurrence(scheduleId: Long) {
        scope.launch {
            val entity = scheduleDao.getScheduleById(scheduleId)
            if (entity != null && entity.repeatType != "NONE") {
                val schedule = entity.toDomain()
                scheduleManager.scheduleAlarm(schedule)
            }
        }
    }

    private fun cancelStopAlarm(context: Context, scheduleId: Long) {
        val stopIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_STOP_SCHEDULED_RECORDING
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() * 2 + 1,
            stopIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (stopPendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(stopPendingIntent)
            stopPendingIntent.cancel()
        }
    }

    private fun disableOneTimeSchedule(scheduleId: Long) {
        scope.launch {
            val entity = scheduleDao.getScheduleById(scheduleId)
            if (entity != null && entity.repeatType == "NONE") {
                val updated = entity.copy(isEnabled = false)
                scheduleDao.updateSchedule(updated)
            }
        }
    }

    companion object {
        const val ACTION_START_SCHEDULED_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_START_SCHEDULED_RECORDING"
        const val ACTION_STOP_SCHEDULED_RECORDING = "com.manoj.backgroundvideorecorder.ACTION_STOP_SCHEDULED_RECORDING"

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
    }
}
