package com.neogenesis.domain.session

import com.neogenesis.domain.model.SessionMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface SessionManager {
    val logoutEvents: SharedFlow<Unit>
    val sessionMetadataFlow: Flow<SessionMetadata?>
    fun getUserId(): String?
    fun saveSession(token: String, userId: String)
    fun clear()
}