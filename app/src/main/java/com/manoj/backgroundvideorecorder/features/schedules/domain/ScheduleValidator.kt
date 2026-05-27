package com.manoj.backgroundvideorecorder.features.schedules.domain

import com.manoj.backgroundvideorecorder.features.schedules.domain.model.Schedule
import java.util.Calendar

object ScheduleValidator {

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    fun validate(newSchedule: Schedule, existingSchedules: List<Schedule>): ValidationResult {
        if (newSchedule.durationSeconds <= 0) {
            return ValidationResult.Error("Duration must be greater than 0 seconds.")
        }
        if (newSchedule.durationSeconds > 7200) {
            return ValidationResult.Error("Duration cannot exceed 2 hours.")
        }

        val now = System.currentTimeMillis()
        if (newSchedule.repeatType == "NONE" && newSchedule.scheduledTimeMillis <= now) {
            return ValidationResult.Error("Cannot schedule a one-time recording in the past.")
        }

        for (existing in existingSchedules) {
            if (existing.id == newSchedule.id || !existing.isEnabled) continue

            if (areOverlapping(newSchedule, existing)) {
                return ValidationResult.Error("This schedule overlaps with an existing schedule starting at ${formatTime(existing.scheduledTimeMillis)}.")
            }
        }

        return ValidationResult.Success
    }

    private fun areOverlapping(s1: Schedule, s2: Schedule): Boolean {
        if (s1.repeatType == "NONE" && s2.repeatType == "NONE") {
            val start1 = s1.scheduledTimeMillis
            val end1 = start1 + s1.durationSeconds * 1000L
            val start2 = s2.scheduledTimeMillis
            val end2 = start2 + s2.durationSeconds * 1000L
            return Math.max(start1, start2) < Math.min(end1, end2)
        }

        val days1 = getActiveDaysOfWeek(s1)
        val days2 = getActiveDaysOfWeek(s2)
        val commonDays = days1.intersect(days2)
        if (commonDays.isEmpty()) {
            return false
        }

        if (s1.repeatType == "NONE" || s2.repeatType == "NONE") {
            val oneTime = if (s1.repeatType == "NONE") s1 else s2
            val recurring = if (s1.repeatType == "NONE") s2 else s1
            
            val calOneTime = Calendar.getInstance().apply { timeInMillis = oneTime.scheduledTimeMillis }
            val dayOfWeekOneTime = calOneTime.get(Calendar.DAY_OF_WEEK)
            if (!getActiveDaysOfWeek(recurring).contains(dayOfWeekOneTime)) {
                return false
            }
        }

        val startSec1 = getTimeOfDaySeconds(s1.scheduledTimeMillis)
        val endSec1 = startSec1 + s1.durationSeconds
        val startSec2 = getTimeOfDaySeconds(s2.scheduledTimeMillis)
        val endSec2 = startSec2 + s2.durationSeconds

        return Math.max(startSec1, startSec2) < Math.min(endSec1, endSec2)
    }

    private fun getActiveDaysOfWeek(schedule: Schedule): Set<Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = schedule.scheduledTimeMillis }
        val startDay = cal.get(Calendar.DAY_OF_WEEK)
        return when (schedule.repeatType) {
            "NONE" -> setOf(startDay)
            "DAILY" -> setOf(1, 2, 3, 4, 5, 6, 7)
            "WEEKDAYS" -> setOf(2, 3, 4, 5, 6)
            "WEEKLY" -> setOf(startDay)
            "CUSTOM" -> {
                try {
                    schedule.daysOfWeek.split(",").map { it.trim().toInt() }.toSet()
                } catch (e: Exception) {
                    setOf(startDay)
                }
            }
            else -> setOf(startDay)
        }
    }

    private fun getTimeOfDaySeconds(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.HOUR_OF_DAY) * 3600 +
               cal.get(Calendar.MINUTE) * 60 +
               cal.get(Calendar.SECOND)
    }

    private fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return String.format("%02d:%02d", hour, minute)
    }
}
