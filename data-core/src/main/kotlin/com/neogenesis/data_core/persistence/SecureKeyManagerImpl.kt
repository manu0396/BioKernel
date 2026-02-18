package com.neogenesis.data_core.persistence

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

class SecureKeyManagerImpl(context: Context) : SecureKeyManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_database_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getOrCreateDatabaseKey(): String {
        val currentKey = sharedPreferences.getString(DB_KEY_ALIAS, null)
        return if (currentKey != null) {
            currentKey
        } else {
            val newKey = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(DB_KEY_ALIAS, newKey).apply()
            newKey
        }
    }

    companion object {
        private const val DB_KEY_ALIAS = "biokernel_db_passphrase"
    }
}


