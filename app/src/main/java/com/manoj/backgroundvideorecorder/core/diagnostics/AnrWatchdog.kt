package com.manoj.backgroundvideorecorder.core.diagnostics

import android.os.Handler
import android.os.Looper
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnrWatchdog @Inject constructor(
    private val auditLogDao: AuditLogDao
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var watchdogThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            return
        }
        AppLogger.i("AnrWatchdog: Starting watchdog thread.")
        watchdogThread = Thread({
            val tick = AtomicBoolean(false)
            val checkRunnable = Runnable { tick.set(true) }

            while (isRunning.get()) {
                tick.set(false)
                mainHandler.post(checkRunnable)

                try {
                    Thread.sleep(5000)
                } catch (e: InterruptedException) {
                    break
                }

                if (!tick.get()) {
                    val mainThread = Looper.getMainLooper().thread
                    val stackTrace = mainThread.stackTrace
                    val builder = StringBuilder("ANR Stall Detected on main thread:\n")
                    for (element in stackTrace) {
                        builder.append("    at ").append(element.toString()).append("\n")
                    }
                    val stackTraceString = builder.toString()
                    
                    AppLogger.e(stackTraceString)
                    
                    scope.launch {
                        try {
                            auditLogDao.insert(
                                AuditLogEntity(
                                    timestamp = System.currentTimeMillis(),
                                    action = "ANR Warning",
                                    detail = "Main thread stalled for >5s. Stacktrace:\n${stackTrace.take(8).joinToString("\n")}"
                                )
                            )
                        } catch (e: Exception) {
                            AppLogger.e(e, "Failed to insert ANR audit log")
                        }
                    }
                }
            }
        }, "BVR-ANR-Watchdog").apply {
            priority = Thread.MIN_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        watchdogThread?.interrupt()
        watchdogThread = null
        AppLogger.i("AnrWatchdog: Stopped watchdog thread.")
    }
}
