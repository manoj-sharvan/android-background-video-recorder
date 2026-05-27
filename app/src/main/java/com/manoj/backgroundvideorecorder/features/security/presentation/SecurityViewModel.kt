package com.manoj.backgroundvideorecorder.features.security.presentation

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.features.security.domain.AuditAction
import com.manoj.backgroundvideorecorder.features.security.domain.AuditLogger
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val auditLogDao: AuditLogDao,
    private val auditLogger: AuditLogger,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityState())
    val state: StateFlow<SecurityState> = _state.asStateFlow()

    init {
        loadSettings()
        // Collect security preference changes
        viewModelScope.launch {
            securityRepository.securityChanges.collect {
                loadSettings()
            }
        }
        // Collect audit logs
        viewModelScope.launch {
            auditLogDao.getRecentLogs(50).collect { logs ->
                _state.update { it.copy(auditLogs = logs) }
            }
        }
    }

    private fun loadSettings() {
        _state.update {
            it.copy(
                isAppLockEnabled = securityRepository.isAppLockEnabled(),
                isBiometricEnabled = securityRepository.isBiometricEnabled(),
                hasPinSet = !securityRepository.getPinHash().isNullOrEmpty(),
                sessionTimeoutMinutes = securityRepository.getSessionTimeoutMinutes(),
                isPrivateNotificationMode = securityRepository.isPrivateNotificationMode(),
                activeLauncherAlias = securityRepository.getActiveLauncherAlias(),
                isEncryptRecordingsEnabled = securityRepository.isEncryptRecordingsEnabled(),
                isHiddenPreviewMode = securityRepository.isHiddenPreviewMode(),
                failedAttempts = securityRepository.getFailedAttempts(),
                cooldownExpiryTime = securityRepository.getCooldownExpiryTime()
            )
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !state.value.hasPinSet) {
            // Cannot enable app lock without a PIN set
            _state.update { it.copy(error = "Please set a PIN first") }
            return
        }
        securityRepository.setAppLockEnabled(enabled)
        auditLogger.log(
            AuditAction.SETTINGS_CHANGE,
            "App lock ${if (enabled) "enabled" else "disabled"}"
        )
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityRepository.setBiometricEnabled(enabled)
        auditLogger.log(
            AuditAction.SETTINGS_CHANGE,
            "Biometric unlock ${if (enabled) "enabled" else "disabled"}"
        )
    }

    fun startPinSetup() {
        _state.update {
            it.copy(
                pinSetupStep = PinSetupStep.ENTER_NEW_PIN,
                tempPin = "",
                error = null
            )
        }
    }

    fun cancelPinSetup() {
        _state.update {
            it.copy(
                pinSetupStep = PinSetupStep.NONE,
                tempPin = "",
                error = null
            )
        }
    }

    fun handlePinInput(pin: String) {
        val currentStep = state.value.pinSetupStep
        if (currentStep == PinSetupStep.ENTER_NEW_PIN) {
            _state.update {
                it.copy(
                    tempPin = pin,
                    pinSetupStep = PinSetupStep.CONFIRM_NEW_PIN,
                    error = null
                )
            }
        } else if (currentStep == PinSetupStep.CONFIRM_NEW_PIN) {
            if (pin == state.value.tempPin) {
                val hash = hashPin(pin)
                securityRepository.setPinHash(hash)
                // Auto enable app lock if first time setting up
                securityRepository.setAppLockEnabled(true)
                _state.update {
                    it.copy(
                        pinSetupStep = PinSetupStep.NONE,
                        tempPin = "",
                        error = null
                    )
                }
                auditLogger.log(AuditAction.SETTINGS_CHANGE, "New secure PIN set successfully")
            } else {
                _state.update {
                    it.copy(
                        pinSetupStep = PinSetupStep.ENTER_NEW_PIN,
                        tempPin = "",
                        error = "PINs do not match. Start over."
                    )
                }
            }
        }
    }

    fun setSessionTimeout(minutes: Int) {
        securityRepository.setSessionTimeoutMinutes(minutes)
        auditLogger.log(AuditAction.SETTINGS_CHANGE, "Session timeout set to $minutes minutes")
    }

    fun setPrivateNotificationMode(enabled: Boolean) {
        securityRepository.setPrivateNotificationMode(enabled)
        auditLogger.log(
            AuditAction.SETTINGS_CHANGE,
            "Private notifications ${if (enabled) "enabled" else "disabled"}"
        )
    }

    fun setEncryptRecordingsEnabled(enabled: Boolean) {
        securityRepository.setEncryptRecordingsEnabled(enabled)
        auditLogger.log(
            AuditAction.SETTINGS_CHANGE,
            "Recording encryption ${if (enabled) "enabled" else "disabled"}"
        )
    }

    fun setHiddenPreviewMode(enabled: Boolean) {
        securityRepository.setHiddenPreviewMode(enabled)
        auditLogger.log(
            AuditAction.SETTINGS_CHANGE,
            "Hidden preview mode ${if (enabled) "enabled" else "disabled"}"
        )
    }

    fun setLauncherAlias(alias: String) {
        val currentAlias = securityRepository.getActiveLauncherAlias()
        if (currentAlias == alias) return

        try {
            switchLauncherAlias(context, alias)
            securityRepository.setActiveLauncherAlias(alias)
            auditLogger.log(AuditAction.SETTINGS_CHANGE, "Launcher disguise changed to $alias")
        } catch (e: Exception) {
            _state.update { it.copy(error = "Failed to switch icon: ${e.localizedMessage}") }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            auditLogDao.clearLogs()
            auditLogger.log(AuditAction.SETTINGS_CHANGE, "Audit logs cleared manually")
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun switchLauncherAlias(context: Context, newAliasName: String) {
        val aliases = listOf(
            "com.manoj.backgroundvideorecorder.MainActivityReal",
            "com.manoj.backgroundvideorecorder.MainActivityCalculator",
            "com.manoj.backgroundvideorecorder.MainActivityNotes",
            "com.manoj.backgroundvideorecorder.MainActivityCalendar"
        )
        val pm = context.packageManager
        val packageName = context.packageName

        for (alias in aliases) {
            val componentName = ComponentName(packageName, alias)
            val isTarget = alias.endsWith(newAliasName, ignoreCase = true)
            val state = if (isTarget) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
