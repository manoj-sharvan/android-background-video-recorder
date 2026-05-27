package com.manoj.backgroundvideorecorder.core.diagnostics

import android.content.Context
import android.content.Intent
import com.manoj.backgroundvideorecorder.features.recording.domain.RecordingManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.system.exitProcess

@Singleton
class CrashHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingManagerProvider: Provider<RecordingManager>
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun register() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Log crash to Timber (FileLoggingTree will capture this)
            Timber.e(throwable, "FATAL CRASH DETECTED in thread ${thread.name}")

            // Write a persistent crash report file for startup recovery & diagnostics
            saveCrashReport(throwable)

            // Safe shut down of CameraX recording to prevent corrupt file handles
            val recordingManager = recordingManagerProvider.get()
            recordingManager.shutdownGracefully()
        } catch (e: Exception) {
            // Avoid looping crashes
        } finally {
            // Attempt graceful restart of the app or fallback to default handler
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                // Fallback to default handler if restart fails
                defaultHandler?.uncaughtException(thread, throwable)
            }
            
            exitProcess(2)
        }
    }

    private fun saveCrashReport(throwable: Throwable) {
        try {
            val crashFile = File(context.filesDir, "crash_report.txt")
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            crashFile.writeText(
                buildString {
                    append("Timestamp: ")
                    append(System.currentTimeMillis())
                    append("\n")
                    append("Thread: ")
                    append(Thread.currentThread().name)
                    append("\n")
                    append("Stacktrace:\n")
                    append(stackTrace)
                }
            )
        } catch (e: Exception) {
            // Fail silently
        }
    }
}
