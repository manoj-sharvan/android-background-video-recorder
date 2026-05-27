package com.manoj.backgroundvideorecorder.features.security.domain

import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

object AuditAction {
    const val UNLOCK_SUCCESS = "UNLOCK_SUCCESS"
    const val UNLOCK_FAILED = "UNLOCK_FAILED"
    const val EXPORT_RECORDING = "EXPORT_RECORDING"
    const val DELETE_RECORDING = "DELETE_RECORDING"
    const val SETTINGS_CHANGE = "SETTINGS_CHANGE"
}

@Singleton
class AuditLogger @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun log(action: String, detail: String = "") {
        scope.launch {
            try {
                val entity = AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    action = action,
                    detail = detail
                )
                auditLogDao.insert(entity)
            } catch (e: Exception) {
                // Ignore or log silently
            }
        }
    }
}
