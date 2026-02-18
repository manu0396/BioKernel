package com.neogenesis.session.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit
import com.neogenesis.domain.model.SessionMetadata
import com.neogenesis.session.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManagerImpl(context: Context) : SessionManager {

    companion object {
        private const val PREFS_NAME = "bio_session_secure"
        private const val KEY_TOKEN = "auth_token"
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

    // Internal state management
    private val _sessionMetadataFlow = MutableStateFlow<SessionMetadata?>(computeInitialSession())
    override val sessionMetadataFlow: StateFlow<SessionMetadata?> = _sessionMetadataFlow.asStateFlow()

    override fun getUserId(): String? {
        val savedId = prefs.getString(KEY_USER_ID, null)
        return if (BuildConfig.FLAVOR == "demo") {
            savedId ?: DEFAULT_DEMO_USER
        } else {
            savedId
        }
    }

    override fun saveSession(token: String, userId: String) {
        prefs.edit(commit = true) {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
        }
        _sessionMetadataFlow.value = SessionMetadata(
            id = 1L,
            patientId = userId
        )
    }

    override fun clear() {
        prefs.edit(commit = true) {
            clear()
        }

        _sessionMetadataFlow.value = null
    }

    private fun computeInitialSession(): SessionMetadata? {
        val token = prefs.getString(KEY_TOKEN, null)
        val userId = getUserId()

        return if (token != null && userId != null) {
            SessionMetadata(
                id = 1L,
                patientId = userId
            )
        } else {
            null
        }
    }
}