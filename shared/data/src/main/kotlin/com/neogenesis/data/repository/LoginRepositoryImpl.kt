package com.neogenesis.data.repository

import android.util.Log
import com.neogenesis.data_core.network.BioApiService
import com.neogenesis.domain.model.SessionMetadata
import com.neogenesis.domain.model.User
import com.neogenesis.domain.repository.LoginRepository
import kotlinx.coroutines.flow.Flow
import com.neogenesis.domain.session.SessionManager

class LoginRepositoryImpl(
    private val apiService: BioApiService,
    private val sessionManager: SessionManager
) : LoginRepository {

    override suspend fun login(user: String, pass: String): Result<User> {
        return try {
            val response = apiService.login(user, pass)

            if (response.success) {
                val domainUser = User(
                    pass = response.token ?: "",
                    user = response.patientId ?: "DEV_MOCK_ID"
                )

                Result.success(domainUser)
            } else {
                Log.e("LoginRepository", "Login failed: ${response.message}")
                Result.failure(Exception(response.message ?: "Credenciales inválidas"))
            }
        } catch (e: Exception) {
            Log.e("LoginRepository", "Critical failure during login flow", e)
            Result.failure(e)
        }
    }

    override fun getSessionMetadata(): Flow<SessionMetadata?> {
        return sessionManager.sessionMetadataFlow
    }
}