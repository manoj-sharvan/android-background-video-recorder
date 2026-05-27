package com.manoj.backgroundvideorecorder.core.diagnostics

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.manoj.backgroundvideorecorder.core.database.AppDatabase
import com.manoj.backgroundvideorecorder.core.database.dao.AuditLogDao
import com.manoj.backgroundvideorecorder.core.database.entity.AuditLogEntity
import com.manoj.backgroundvideorecorder.core.common.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupIntegrityValidator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val auditLogDao: AuditLogDao
) {
    suspend fun validate(): Boolean = withContext(Dispatchers.IO) {
        var allOk = true

        val storageOk = checkStorageIntegrity()
        if (!storageOk) {
            allOk = false
            logFailure("Storage Integrity Failure", "Unable to read/write files to internal storage.")
        }

        val dbOk = checkDatabaseIntegrity()
        if (!dbOk) {
            allOk = false
            logFailure("Database Integrity Failure", "SQLite database integrity check failed or threw an exception.")
        }

        val keyStoreOk = checkKeyStoreIntegrity()
        if (!keyStoreOk) {
            allOk = false
            logFailure("KeyStore Integrity Failure", "AndroidKeyStore is not accessible or failed key generation.")
        }

        if (allOk) {
            AppLogger.i("StartupIntegrityValidator: All checks passed (Storage, DB, KeyStore).")
        } else {
            AppLogger.e("StartupIntegrityValidator: Integrity check failures detected!")
        }

        allOk
    }

    private fun checkStorageIntegrity(): Boolean {
        return try {
            val dir = context.filesDir
            if (!dir.exists() && !dir.mkdirs()) return false
            val tempFile = File(dir, "integrity_test_${System.currentTimeMillis()}.tmp")
            tempFile.writeText("test")
            val content = tempFile.readText()
            tempFile.delete()
            content == "test"
        } catch (e: Exception) {
            AppLogger.e(e, "Storage integrity check threw exception")
            false
        }
    }

    private fun checkDatabaseIntegrity(): Boolean {
        return try {
            appDatabase.openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    result.equals("ok", ignoreCase = true)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            AppLogger.e(e, "Database integrity check threw exception")
            false
        }
    }

    private fun checkKeyStoreIntegrity(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "startup_integrity_test_key"
            if (!keyStore.containsAlias(alias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            entry != null
        } catch (e: Exception) {
            AppLogger.e(e, "KeyStore integrity check threw exception")
            false
        }
    }

    private suspend fun logFailure(action: String, detail: String) {
        AppLogger.e("Integrity Failure: $action - $detail")
        try {
            auditLogDao.insert(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    action = action,
                    detail = detail
                )
            )
        } catch (e: Exception) {
            AppLogger.e(e, "Failed to write startup failure to audit log")
        }
    }
}
