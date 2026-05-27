package com.manoj.backgroundvideorecorder.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.util.Locale

object OemOptimizationHelper {

    fun isMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
        return manufacturer.contains("xiaomi") || manufacturer.contains("poco") || manufacturer.contains("redmi")
    }

    fun getAutoStartIntent(context: Context): Intent? {
        if (!isMiui()) return null
        return try {
            val intent = Intent()
            intent.component = android.content.ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent
        } catch (e: Exception) {
            null
        }
    }

    fun getBatteryOptimizationIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }
}
