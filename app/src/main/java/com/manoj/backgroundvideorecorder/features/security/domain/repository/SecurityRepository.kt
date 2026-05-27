package com.manoj.backgroundvideorecorder.features.security.domain.repository

import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    fun isAppLockEnabled(): Boolean
    fun setAppLockEnabled(enabled: Boolean)

    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)

    fun getPinHash(): String?
    fun setPinHash(hash: String?)

    fun getSessionTimeoutMinutes(): Int
    fun setSessionTimeoutMinutes(minutes: Int)

    fun isPrivateNotificationMode(): Boolean
    fun setPrivateNotificationMode(enabled: Boolean)

    fun getActiveLauncherAlias(): String
    fun setActiveLauncherAlias(alias: String)

    fun isEncryptRecordingsEnabled(): Boolean
    fun setEncryptRecordingsEnabled(enabled: Boolean)

    fun isHiddenPreviewMode(): Boolean
    fun setHiddenPreviewMode(enabled: Boolean)

    fun getFailedAttempts(): Int
    fun setFailedAttempts(attempts: Int)

    fun getCooldownExpiryTime(): Long
    fun setCooldownExpiryTime(timeMillis: Long)

    val securityChanges: Flow<Unit>
}
