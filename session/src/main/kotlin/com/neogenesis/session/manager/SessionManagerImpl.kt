package com.neogenesis.session.manager

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import com.neogenesis.domain.model.SessionMetadata
import com.neogenesis.data_core.persistence.CryptoManager
import com.neogenesis.domain.session.SessionManager
import com.neogenesis.session.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManagerImpl(
    context: Context,
    private val cryptoManager: CryptoManager
) : SessionManager {
    private val _logoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val logoutEvents = _logoutEvents.asSharedFlow()

    companion object {
        private const val PREFS_NAME = "bio_session_secure"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_IV = "auth_iv"
        private const val KEY_USER_ID = "user_id"
        private const val DEFAULT_DEMO_USER = "BIO-USER-DEMO-001"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _sessionMetadataFlow = MutableStateFlow<SessionMetadata?>(computeInitialSession())
    override val sessionMetadataFlow: StateFlow<SessionMetadata?> = _sessionMetadataFlow.asStateFlow()

    override fun getUserId(): String? {
        val savedId = prefs.getString(KEY_USER_ID, null)
        return if (BuildConfig.FLAVOR == "demo") savedId ?: DEFAULT_DEMO_USER else savedId
    }

    override fun saveSession(token: String, userId: String) {
        val (encrypted, iv) = cryptoManager.encrypt(token)
        val tokenBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        prefs.edit(commit = true) {
            putString(KEY_TOKEN, tokenBase64)
            putString(KEY_IV, ivBase64)
            putString(KEY_USER_ID, userId)
        }

        _sessionMetadataFlow.value = SessionMetadata(id = 1L, patientId = userId)
    }

    override fun clear() {
        prefs.edit(commit = true) { clear() }
        _sessionMetadataFlow.value = null
        _logoutEvents.tryEmit(Unit)
    }

    private fun computeInitialSession(): SessionMetadata? {
        val tokenBase64 = prefs.getString(KEY_TOKEN, null) ?: return null
        val ivBase64 = prefs.getString(KEY_IV, null) ?: return null
        val userId = getUserId() ?: return null

        return try {
            val encrypted = Base64.decode(tokenBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            cryptoManager.decrypt(encrypted, iv)
            SessionMetadata(id = 1L, patientId = userId)
        } catch (e: Exception) {
            null
        }
    }
}