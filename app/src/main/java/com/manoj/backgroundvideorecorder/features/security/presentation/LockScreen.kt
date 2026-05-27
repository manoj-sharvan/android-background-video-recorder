package com.manoj.backgroundvideorecorder.features.security.presentation

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.manoj.backgroundvideorecorder.features.security.domain.AuditAction
import com.manoj.backgroundvideorecorder.features.security.domain.AuditLogger
import com.manoj.backgroundvideorecorder.features.security.domain.BiometricAuthManager
import com.manoj.backgroundvideorecorder.features.security.domain.BiometricAuthResult
import com.manoj.backgroundvideorecorder.features.security.domain.repository.SecurityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

@Composable
fun LockScreen(
    onUnlockSuccess: () -> Unit,
    securityRepository: SecurityRepository,
    biometricAuthManager: BiometricAuthManager,
    auditLogger: AuditLogger,
    activity: FragmentActivity
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinInput by remember { mutableStateOf("") }
    
    // Cooldown state
    var cooldownRemainingSecs by remember { mutableStateOf(0L) }
    var failedAttemptsState by remember { mutableStateOf(securityRepository.getFailedAttempts()) }

    // Check & tick cooldown
    LaunchedEffect(key1 = Unit) {
        while (true) {
            val expiry = securityRepository.getCooldownExpiryTime()
            val now = System.currentTimeMillis()
            if (expiry > now) {
                cooldownRemainingSecs = (expiry - now) / 1000 + 1
            } else {
                cooldownRemainingSecs = 0L
                if (failedAttemptsState >= 5) {
                    // Reset attempts if cooldown expires
                    securityRepository.setFailedAttempts(0)
                    failedAttemptsState = 0
                }
            }
            delay(1000)
        }
    }

    // Auto-trigger biometric authentication if enabled
    LaunchedEffect(key1 = Unit) {
        if (securityRepository.isBiometricEnabled() && biometricAuthManager.isBiometricAvailable()) {
            // Give a tiny delay for layout transition
            delay(300)
            val result = biometricAuthManager.authenticate(
                activity = activity,
                title = "App Lock",
                subtitle = "Authenticate to open Background Video Recorder"
            )
            if (result is BiometricAuthResult.Success) {
                auditLogger.log(AuditAction.UNLOCK_SUCCESS, "Unlocked via Biometrics")
                securityRepository.setFailedAttempts(0)
                onUnlockSuccess()
            }
        }
    }

    fun handlePinKey(digit: String) {
        if (cooldownRemainingSecs > 0) {
            Toast.makeText(context, "App locked. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }
        if (pinInput.length < 6) {
            pinInput += digit
        }
        // Auto check when length matches typical PIN length (usually 4-6)
        val expectedHash = securityRepository.getPinHash() ?: return
        val currentHash = hashPin(pinInput)
        
        if (currentHash == expectedHash) {
            // Success
            securityRepository.setFailedAttempts(0)
            auditLogger.log(AuditAction.UNLOCK_SUCCESS, "Unlocked via PIN")
            onUnlockSuccess()
        } else if (pinInput.length >= 6 || (pinInput.length >= 4 && expectedHash.length == 64 && pinInput.length == expectedHashPinLength(expectedHash, pinInput))) {
            // Fail
            val newAttempts = failedAttemptsState + 1
            securityRepository.setFailedAttempts(newAttempts)
            failedAttemptsState = newAttempts
            pinInput = ""
            
            if (newAttempts >= 5) {
                val cooldownExpiry = System.currentTimeMillis() + 60_000 // 1 minute cooldown
                securityRepository.setCooldownExpiryTime(cooldownExpiry)
                cooldownRemainingSecs = 60
                auditLogger.log(AuditAction.UNLOCK_FAILED, "Rate limit reached. 1 minute cooldown initiated.")
                Toast.makeText(context, "Too many incorrect attempts. App locked for 1 minute.", Toast.LENGTH_LONG).show()
            } else {
                auditLogger.log(AuditAction.UNLOCK_FAILED, "Incorrect PIN attempt ($newAttempts/5)")
                Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val appTitle = when (securityRepository.getActiveLauncherAlias()) {
                "CALCULATOR" -> "Calculator"
                "NOTES" -> "Notes"
                "CALENDAR" -> "Calendar"
                else -> "Secure Recorder"
            }
            val appIcon = when (securityRepository.getActiveLauncherAlias()) {
                "CALCULATOR" -> Icons.Default.Lock
                "NOTES" -> Icons.Default.Lock
                "CALENDAR" -> Icons.Default.Lock
                else -> Icons.Default.Lock
            }

            Icon(
                imageVector = appIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = appTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (cooldownRemainingSecs > 0) {
                    "App locked. Try again in $cooldownRemainingSecs seconds."
                } else {
                    "Enter your PIN to access files"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (cooldownRemainingSecs > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Dots indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            val dotsCount = if (pinInput.isEmpty()) 4 else pinInput.length
            for (i in 0 until 6) {
                val active = i < pinInput.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                )
            }
        }

        // Keypad Grid Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "backspace")
            )

            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowKeys.forEach { key ->
                        if (key.isEmpty()) {
                            // Biometric trigger placeholder
                            if (securityRepository.isBiometricEnabled() && biometricAuthManager.isBiometricAvailable()) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val result = biometricAuthManager.authenticate(
                                                activity = activity,
                                                title = "App Lock",
                                                subtitle = "Authenticate to open Background Video Recorder"
                                            )
                                            if (result is BiometricAuthResult.Success) {
                                                auditLogger.log(AuditAction.UNLOCK_SUCCESS, "Unlocked via Biometrics")
                                                securityRepository.setFailedAttempts(0)
                                                onUnlockSuccess()
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Auth",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(72.dp))
                            }
                        } else if (key == "backspace") {
                            IconButton(
                                onClick = {
                                    if (pinInput.isNotEmpty()) {
                                        pinInput = pinInput.substring(0, pinInput.length - 1)
                                    }
                                },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { handlePinKey(key) }
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun hashPin(pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(pin.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}

private fun expectedHashPinLength(expectedHash: String, currentInput: String): Int {
    // If input length is 4, does it match hash of length 4?
    if (hashPin(currentInput) == expectedHash) {
        return currentInput.length
    }
    // Standard PIN checks fallback to 4 digits if not fully matched
    return 4
}
