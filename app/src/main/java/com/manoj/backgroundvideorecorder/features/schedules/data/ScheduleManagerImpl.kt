package com.manoj.backgroundvideorecorder.features.schedules.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.manoj.backgroundvideorecorder.background.receiver.ScheduleAlarmReceiver
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.ScheduleDao
import com.manoj.backgroundvideorecorder.features.schedules.data.mapper.toDomain
import com.manoj.backgroundvideorecorder.features.schedules.data.mapper.toEntity
import com.manoj.backgroundvideorecorder.features.schedules.domain.ScheduleManager
import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleDao: ScheduleDao
) : ScheduleManager {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("ScheduleExactAlarm")
    override fun scheduleAlarm(schedule: Schedule) {
        if (!schedule.isEnabled) {
            cancelAlarm(schedule)
            return
        }

        val now = System.currentTimeMillis()
        var targetStartMillis = schedule.scheduledTimeMillis

        // If target time has passed and it's a recurring schedule, calculate next trigger
        if (targetStartMillis <= now && schedule.repeatType != "NONE") {
            targetStartMillis = calculateNextTriggerTime(schedule.scheduledTimeMillis, schedule.repeatType, schedule.daysOfWeek)
            // Update next trigger time in Room DB
            scope.launch {
                val updatedSchedule = schedule.copy(scheduledTimeMillis = targetStartMillis, nextTriggerTime = targetStartMillis)
                scheduleDao.updateSchedule(updatedSchedule.toEntity())
            }
        }

        // Only program alarm if it's in the future
        if (targetStartMillis > now) {
            AppLogger.i("ScheduleManager: Programming START alarm for schedule ${schedule.id} at $targetStartMillis")

            // 1. START PendingIntent
            val startIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ScheduleAlarmReceiver.ACTION_START_SCHEDULED_RECORDING
                putExtra(ScheduleAlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
                putExtra(ScheduleAlarmReceiver.EXTRA_DURATION_SECONDS, schedule.durationSeconds)
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.id.toInt() * 2,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2. STOP PendingIntent
            val stopIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ScheduleAlarmReceiver.ACTION_STOP_SCHEDULED_RECORDING
                putExtra(ScheduleAlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                schedule.id.toInt() * 2 + 1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule START alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetStartMillis, startPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetStartMillis, startPendingIntent)
            }

            // Schedule STOP alarm
            val targetStopMillis = targetStartMillis + (schedule.durationSeconds * 1000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetStopMillis, stopPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetStopMillis, stopPendingIntent)
            }
        } else {
            AppLogger.w("ScheduleManager: Schedule time has passed and cannot be scheduled (One-time). ID: ${schedule.id}")
        }
    }

    override fun cancelAlarm(schedule: Schedule) {
        AppLogger.i("ScheduleManager: Canceling alarms for schedule ${schedule.id}")

        val startIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_START_SCHEDULED_RECORDING
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt() * 2,
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
            startPendingIntent.cancel()
        }

        val stopIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_STOP_SCHEDULED_RECORDING
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt() * 2 + 1,
            stopIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (stopPendingIntent != null) {
            alarmManager.cancel(stopPendingIntent)
            stopPendingIntent.cancel()
        }
    }

    override fun rescheduleAllActiveAlarms() {
        scope.launch {
            val allEntities = scheduleDao.getAllSchedules().firstOrNull() ?: emptyList()
            val activeSchedules = allEntities.map { it.toDomain() }.filter { it.isEnabled }
            AppLogger.i("ScheduleManager: Rescheduling all active schedules (${activeSchedules.size} items)")
            activeSchedules.forEach { scheduleAlarm(it) }
        }
    }

    private fun calculateNextTriggerTime(scheduleTimeMillis: Long, repeatType: String, daysOfWeek: String): Long {
        val now = System.currentTimeMillis()
        if (scheduleTimeMillis > now) return scheduleTimeMillis

        val calendar = Calendar.getInstance().apply {
            timeInMillis = scheduleTimeMillis
        }

        while (calendar.timeInMillis <= now) {
            when (repeatType) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKDAYS" -> {
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                }
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "CUSTOM" -> {
                    val activeDays = try {
                        daysOfWeek.split(",").map { it.trim().toInt() }.toSet()
                    } catch (e: Exception) {
                        emptySet()
                    }

                    if (activeDays.isEmpty()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } else {
                        do {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        } while (!activeDays.contains(calendar.get(Calendar.DAY_OF_WEEK)))
                    }
                }
                else -> return scheduleTimeMillis
            }
        }
        return calendar.timeInMillis
    }
}
