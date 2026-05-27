package com.manoj.backgroundvideorecorder.core.diagnostics

import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logEvent(event: String, parameters: Map<String, String> = emptyMap()) {
        val paramsString = parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }
        AppLogger.i("Analytics Event: $event ($paramsString)")

        scope.launch {
            try {
                auditLogDao.insert(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        action = "Analytics: $event",
                        detail = paramsString.ifEmpty { "No details provided" }
                    )
                )
            } catch (e: Exception) {
                AppLogger.e(e, "Failed to write analytics event to DB")
            }
        }
    }
}
