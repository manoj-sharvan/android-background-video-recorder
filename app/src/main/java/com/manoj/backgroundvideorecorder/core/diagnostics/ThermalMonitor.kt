package com.manoj.backgroundvideorecorder.core.diagnostics

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auditLogDao: AuditLogDao
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var thermalListener: Any? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = powerManager ?: return
            try {
                val listener = PowerManager.OnThermalStatusChangedListener { status ->
                    val statusText = when (status) {
                        PowerManager.THERMAL_STATUS_NONE -> "NONE"
                        PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                        PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                        PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                        PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                        else -> "UNKNOWN ($status)"
                    }
                    AppLogger.w("Thermal status changed: $statusText")

                    if (status >= PowerManager.THERMAL_STATUS_SEVERE) {
                        scope.launch {
                            try {
                                auditLogDao.insert(
                                    AuditLogEntity(
                                        timestamp = System.currentTimeMillis(),
                                        action = "Thermal Warning",
                                        detail = "High thermal status detected: $statusText"
                                    )
                                )
                            } catch (e: Exception) {
                                AppLogger.e(e, "Failed to log thermal warning to DB")
                            }
                        }
                    }
                }
                pm.addThermalStatusListener(context.mainExecutor, listener)
                thermalListener = listener
                AppLogger.i("ThermalMonitor: Registered OnThermalStatusChangedListener.")
            } catch (e: Exception) {
                AppLogger.e(e, "ThermalMonitor: Failed to register listener.")
            }
        } else {
            AppLogger.i("ThermalMonitor: Thermal status API not supported on this SDK level.")
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = powerManager ?: return
            val listener = thermalListener as? PowerManager.OnThermalStatusChangedListener ?: return
            try {
                pm.removeThermalStatusListener(listener)
                thermalListener = null
                AppLogger.i("ThermalMonitor: Unregistered OnThermalStatusChangedListener.")
            } catch (e: Exception) {
                AppLogger.e(e, "ThermalMonitor: Failed to remove listener.")
            }
        }
    }
}
