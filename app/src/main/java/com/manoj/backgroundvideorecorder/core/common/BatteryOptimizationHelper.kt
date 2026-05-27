package com.manoj.backgroundvideorecorder.core.common

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun getBatteryOptimizationIntent(): Intent {
        val intent = Intent()
        if (isIgnoringBatteryOptimizations()) {
            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        } else {
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:${context.packageName}")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.lowercase(Locale.US)
    }

    fun getOemBatterySettingsIntent(): Intent? {
        val manufacturer = getDeviceManufacturer()
        val intents = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.powerkeeper.ui.PowerKeeperSettingsActivity"))
            )
            manufacturer.contains("samsung") -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            )
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> listOf(
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.page.NewPowerManagerActivity")),
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
            )
            manufacturer.contains("vivo") -> listOf(
                Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.safeguard.PurseSuperPowerSaver"))
            )
            manufacturer.contains("huawei") -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
            )
            else -> emptyList()
        }

        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isIntentResolvable(intent)) {
                return intent
            }
        }

        return null
    }

    private fun isIntentResolvable(intent: Intent): Boolean {
        return try {
            val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
            resolveInfo.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
