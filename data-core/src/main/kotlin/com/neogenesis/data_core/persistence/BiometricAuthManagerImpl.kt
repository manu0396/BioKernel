package com.neogenesis.data_core.persistence

import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class BiometricAuthManagerImpl(
    private val activity: FragmentActivity
) : BiometricAuthManager {

    override suspend fun authenticateAndGetKey(): Result<String> {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val executor = ContextCompat.getMainExecutor(activity)

                val callback = object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) {
                            continuation.resume(Result.success("Authentication Succeeded"))
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception(errString.toString())))
                        }
                    }

                    override fun onAuthenticationFailed() {
                    }
                }

                val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle("BioKernel Security")
                    .setSubtitle("Retina Data Decryption")
                    .setNegativeButtonText("Cancel")
                    .setConfirmationRequired(false)
                    .build()

                try {
                    val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor, callback)
                    biometricPrompt.authenticate(promptInfo)

                    continuation.invokeOnCancellation {
                        biometricPrompt.cancelAuthentication()
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(e))
                    }
                }
            }
        }
    }
}


