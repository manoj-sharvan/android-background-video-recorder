package com.manoj.backgroundvideorecorder.features.security.domain

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: String) : BiometricAuthResult()
    object Failed : BiometricAuthResult()
    object Unavailable : BiometricAuthResult()
}

class BiometricAuthManager(private val context: Context) {

    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    suspend fun authenticate(activity: FragmentActivity, title: String, subtitle: String): BiometricAuthResult {
        if (!isBiometricAvailable()) {
            return BiometricAuthResult.Unavailable
        }

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If prompt was cancelled by user, we can handle it
                    continuation.resume(BiometricAuthResult.Error(errorCode, errString.toString()))
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    continuation.resume(BiometricAuthResult.Success)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    continuation.resume(BiometricAuthResult.Failed)
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                biometricPrompt.cancelAuthentication()
            }
        }
    }
}
