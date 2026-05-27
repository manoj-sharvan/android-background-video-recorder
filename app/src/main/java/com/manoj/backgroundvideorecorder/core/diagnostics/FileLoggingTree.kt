package com.manoj.backgroundvideorecorder.core.diagnostics

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FileLoggingTree(context: Context) : Timber.DebugTree() {

    private val logDir = File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
    private val logFile = File(logDir, "app_logs.txt")
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        
        val priorityStr = when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }

        val logEntry = buildString {
            append(dateFormat.format(Date()))
            append(" [")
            append(priorityStr)
            append("] ")
            if (tag != null) {
                append(tag)
                append(": ")
            }
            append(message)
            if (t != null) {
                append("\n")
                append(Log.getStackTraceString(t))
            }
            append("\n")
        }

        executor.submit {
            try {
                // Keep log file under 5MB, rotate if needed
                if (logFile.exists() && logFile.length() > 5 * 1024 * 1024) {
                    val backupFile = File(logDir, "app_logs_old.txt")
                    if (backupFile.exists()) backupFile.delete()
                    logFile.renameTo(backupFile)
                }
                FileWriter(logFile, true).use { writer ->
                    writer.write(logEntry)
                }
            } catch (e: Exception) {
                // Fail silently to avoid crash loops
            }
        }
    }

    fun getLogFile(): File = logFile
}
