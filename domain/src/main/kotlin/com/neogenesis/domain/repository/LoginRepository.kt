package com.neogenesis.domain.repository

import com.neogenesis.domain.model.SessionMetadata
import kotlinx.coroutines.flow.Flow
import com.neogenesis.domain.model.User

interface LoginRepository {
    suspend fun login(user: String, token: String): Result<User>
    fun getSessionMetadata(): Flow<SessionMetadata?>
}






