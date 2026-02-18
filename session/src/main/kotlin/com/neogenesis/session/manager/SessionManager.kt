package com.neogenesis.session.manager

import com.neogenesis.domain.model.SessionMetadata
import kotlinx.coroutines.flow.Flow

interface SessionManager {
    val sessionMetadataFlow: Flow<SessionMetadata?>
    fun getUserId(): String?
    fun saveSession(token: String, userId: String)
    fun clear()
}


