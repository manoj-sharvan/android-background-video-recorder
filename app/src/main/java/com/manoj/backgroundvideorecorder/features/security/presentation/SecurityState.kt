package com.manoj.backgroundvideorecorder.features.security.presentation

import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity

data class SecurityState(
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val hasPinSet: Boolean = false,
    val sessionTimeoutMinutes: Int = 5,
    val isPrivateNotificationMode: Boolean = false,
    val activeLauncherAlias: String = "REAL",
    val isEncryptRecordingsEnabled: Boolean = false,
    val isHiddenPreviewMode: Boolean = false,
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val error: String? = null,
    val pinSetupStep: PinSetupStep = PinSetupStep.NONE,
    val tempPin: String = "",
    val failedAttempts: Int = 0,
    val cooldownExpiryTime: Long = 0L
)

enum class PinSetupStep {
    NONE,
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN
}
