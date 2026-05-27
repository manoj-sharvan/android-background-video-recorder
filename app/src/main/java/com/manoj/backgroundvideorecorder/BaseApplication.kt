package com.manoj.backgroundvideorecorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashHandler: com.manoj.backgroundvideorecorder.core.diagnostics.CrashHandler

    @Inject
    lateinit var storageCleanupManager: com.manoj.backgroundvideorecorder.features.storage.domain.StorageCleanupManager

    @Inject
    lateinit var startupIntegrityValidator: com.manoj.backgroundvideorecorder.core.diagnostics.StartupIntegrityValidator

    @Inject
    lateinit var anrWatchdog: com.manoj.backgroundvideorecorder.core.diagnostics.AnrWatchdog

    @Inject
    lateinit var thermalMonitor: com.manoj.backgroundvideorecorder.core.diagnostics.ThermalMonitor

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging and file logger tree
        Timber.plant(com.manoj.backgroundvideorecorder.core.diagnostics.FileLoggingTree(this))
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
            
            // Set up StrictMode in debug builds
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        // Register custom global crash handler
        crashHandler.register()

        // Start ANR watchdog and thermal monitoring
        anrWatchdog.start()
        thermalMonitor.start()

        // Create Notification Channel for Foreground Service
        createNotificationChannel()

        // Startup integrity checks and orphan repairs
        val crashFile = java.io.File(filesDir, "crash_report.txt")
        if (crashFile.exists()) {
            val content = try { crashFile.readText() } catch (e: Exception) { "" }
            Timber.e("Previous crash detected on startup: %s", content)
            crashFile.delete()
        }

        // Run async storage orphan repair and startup integrity checks
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                startupIntegrityValidator.validate()
            } catch (e: Exception) {
                Timber.e(e, "Failed to run startup integrity checks")
            }
            try {
                storageCleanupManager.detectAndRepairOrphans()
            } catch (e: Exception) {
                Timber.e(e, "Failed to run startup orphan repair")
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = CHANNEL_ID
            val channelName = "Background Recording Channel"
            val channelDescription = "Used for showing recording status notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "background_recording_channel"
    }
}
