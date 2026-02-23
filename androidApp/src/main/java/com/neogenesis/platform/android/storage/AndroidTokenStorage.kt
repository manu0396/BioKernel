package com.neogenesis.platform.android.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.neogenesis.platform.shared.network.TokenStorage

class AndroidTokenStorage(context: Context) : TokenStorage {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "neogenesis_tokens",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun readAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override fun readRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override fun writeTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_ACCESS).remove(KEY_REFRESH).apply()
    }

    private companion object {
        private const val KEY_ACCESS = "accessToken"
        private const val KEY_REFRESH = "refreshToken"
    }
}
