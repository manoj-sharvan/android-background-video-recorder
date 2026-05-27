package com.manoj.backgroundvideorecorder.features.security.domain

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class RecordingEncryptionManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "recording_encryption_key"

    init {
        getOrCreateSecretKey()
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (keyStore.containsAlias(keyAlias)) {
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptFile(inputFile: File, outputFile: File) {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv

        inputFile.inputStream().use { input ->
            outputFile.outputStream().use { output ->
                output.write(iv.size)
                output.write(iv)

                val cipherOutputStream = CipherOutputStream(output, cipher)
                val buffer = ByteArray(8192)
                try {
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        cipherOutputStream.write(buffer, 0, bytesRead)
                    }
                } finally {
                    buffer.fill(0) // Secure memory cleanup
                    cipherOutputStream.close()
                }
            }
        }
    }

    fun decryptFile(inputFile: File, outputFile: File) {
        val key = getOrCreateSecretKey()
        inputFile.inputStream().use { input ->
            val ivSize = input.read()
            if (ivSize <= 0 || ivSize > 128) {
                throw IllegalArgumentException("Invalid IV size in encrypted file: $ivSize")
            }
            val iv = ByteArray(ivSize)
            val readBytes = input.read(iv)
            if (readBytes != ivSize) {
                throw IllegalArgumentException("Could not read full IV from encrypted file")
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            outputFile.outputStream().use { output ->
                val cipherInputStream = CipherInputStream(input, cipher)
                val buffer = ByteArray(8192)
                try {
                    var bytesRead: Int
                    while (cipherInputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                } finally {
                    buffer.fill(0) // Secure memory cleanup
                    cipherInputStream.close()
                }
            }
        }
    }
}
