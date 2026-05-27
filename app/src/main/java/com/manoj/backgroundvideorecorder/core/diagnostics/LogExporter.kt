package com.manoj.backgroundvideorecorder.core.diagnostics

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auditLogDao: AuditLogDao,
    private val diagnosticsManager: DiagnosticsManager
) {

    suspend fun exportLogs(): Uri? = withContext(Dispatchers.IO) {
        try {
            val recordingsDir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
            val zipFile = File(recordingsDir, "bvr_diagnostics_report.zip")
            if (zipFile.exists()) {
                zipFile.delete()
            }

            // Get device metadata details
            val metadataFile = File(context.cacheDir, "device_metadata.txt")
            writeMetadata(metadataFile)

            // Get audit logs formatted as text
            val auditLogFile = File(context.cacheDir, "audit_history.txt")
            writeAuditLogs(auditLogFile)

            // Get the app logs file (from FileLoggingTree)
            val logDir = File(context.filesDir, "logs")
            val appLogFile = File(logDir, "app_logs.txt")

            // Zip the files
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                addFileToZip(zos, metadataFile, "device_metadata.txt")
                addFileToZip(zos, auditLogFile, "audit_history.txt")
                if (appLogFile.exists()) {
                    addFileToZip(zos, appLogFile, "app_logs.txt")
                }
            }

            // Cleanup temp cache files
            metadataFile.delete()
            auditLogFile.delete()

            // Return Uri via FileProvider
            FileProvider.getUriForFile(
                context,
                "com.manoj.backgroundvideorecorder.fileprovider",
                zipFile
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun writeMetadata(file: File) {
        val stats = diagnosticsManager.getLatestDiagnostics()
        FileWriter(file).use { writer ->
            writer.write("Background Video Recorder Diagnostics Report\n")
            writer.write("=========================================\n")
            writer.write("Device: ${Build.DEVICE}\n")
            writer.write("Model: ${Build.MODEL}\n")
            writer.write("Manufacturer: ${Build.MANUFACTURER}\n")
            writer.write("Brand: ${Build.BRAND}\n")
            writer.write("Android OS Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            writer.write("Product: ${Build.PRODUCT}\n")
            writer.write("=========================================\n")
            writer.write("Memory Info:\n")
            writer.write("  Total Heap Size: ${stats.memory.totalMemoryBytes / 1024 / 1024} MB\n")
            writer.write("  Available RAM: ${stats.memory.availableMemoryBytes / 1024 / 1024} MB\n")
            writer.write("  App Heap Usage: ${stats.memory.appUsedHeapBytes / 1024 / 1024} MB\n")
            writer.write("  App Max Heap: ${stats.memory.appMaxHeapBytes / 1024 / 1024} MB\n")
            writer.write("=========================================\n")
            writer.write("Battery Info:\n")
            writer.write("  Level: ${stats.battery.levelPercent}%\n")
            writer.write("  Charging: ${stats.battery.isCharging}\n")
            writer.write("  Temperature: ${stats.battery.temperatureCelsius}°C\n")
            writer.write("  Voltage: ${stats.battery.voltageMv} mV\n")
            writer.write("=========================================\n")
            writer.write("Storage Info:\n")
            writer.write("  Total Storage: ${stats.storage.totalBytes / 1024 / 1024} MB\n")
            writer.write("  Available Storage: ${stats.storage.availableBytes / 1024 / 1024} MB\n")
            writer.write("  Used Percent: ${stats.storage.usedPercent}%\n")
        }
    }

    private suspend fun writeAuditLogs(file: File) {
        val logs = auditLogDao.getRecentLogs(500).first()
        FileWriter(file).use { writer ->
            writer.write("Background Video Recorder Access Audit History\n")
            writer.write("=========================================\n")
            if (logs.isEmpty()) {
                writer.write("No audit entries recorded.\n")
            } else {
                for (log in logs) {
                    writer.write("[${log.timestamp}] [${log.action}] ${log.detail}\n")
                }
            }
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        file.inputStream().use { input ->
            input.copyTo(zos)
        }
        zos.closeEntry()
    }
}
