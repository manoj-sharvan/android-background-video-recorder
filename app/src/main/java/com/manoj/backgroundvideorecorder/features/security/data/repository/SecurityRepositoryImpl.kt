package com.manoj.backgroundvideorecorder.features.security.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecurityRepository {

    private val prefs = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_app_settings_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to initialize EncryptedSharedPreferences. Falling back to plain SharedPreferences.")
        context.getSharedPreferences("secure_app_settings_prefs_fallback", Context.MODE_PRIVATE)
    }

    private val _securityChanges = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 8)
    override val securityChanges: Flow<Unit> = _securityChanges.asSharedFlow()

    private fun notifyChanged() {
        _securityChanges.tryEmit(Unit)
    }

    override fun isAppLockEnabled(): Boolean = prefs.getBoolean("app_lock_enabled", false)
    override fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
        notifyChanged()
    }

    override fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)
    override fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        notifyChanged()
    }

    override fun getPinHash(): String? = prefs.getString("pin_hash", null)
    override fun setPinHash(hash: String?) {
        prefs.edit().putString("pin_hash", hash).apply()
        notifyChanged()
    }

    override fun getSessionTimeoutMinutes(): Int = prefs.getInt("session_timeout_minutes", 5)
    override fun setSessionTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt("session_timeout_minutes", minutes).apply()
        notifyChanged()
    }

    override fun isPrivateNotificationMode(): Boolean = prefs.getBoolean("private_notification_mode", false)
    override fun setPrivateNotificationMode(enabled: Boolean) {
        prefs.edit().putBoolean("private_notification_mode", enabled).apply()
        notifyChanged()
    }

    override fun getActiveLauncherAlias(): String = prefs.getString("active_launcher_alias", "REAL") ?: "REAL"
    override fun setActiveLauncherAlias(alias: String) {
        prefs.edit().putString("active_launcher_alias", alias).apply()
        notifyChanged()
    }

    override fun isEncryptRecordingsEnabled(): Boolean = prefs.getBoolean("encrypt_recordings_enabled", false)
    override fun setEncryptRecordingsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("encrypt_recordings_enabled", enabled).apply()
        notifyChanged()
    }

    override fun isHiddenPreviewMode(): Boolean = prefs.getBoolean("hidden_preview_mode", false)
    override fun setHiddenPreviewMode(enabled: Boolean) {
        prefs.edit().putBoolean("hidden_preview_mode", enabled).apply()
        notifyChanged()
    }

    override fun getFailedAttempts(): Int = prefs.getInt("failed_attempts", 0)
    override fun setFailedAttempts(attempts: Int) {
        prefs.edit().putInt("failed_attempts", attempts).apply()
        notifyChanged()
    }

    override fun getCooldownExpiryTime(): Long = prefs.getLong("cooldown_expiry_time", 0L)
    override fun setCooldownExpiryTime(timeMillis: Long) {
        prefs.edit().putLong("cooldown_expiry_time", timeMillis).apply()
        notifyChanged()
    }
}
