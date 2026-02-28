package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.LoginRequestDto
import com.neogenesis.platform.data.api.RegisterRequestDto
import com.neogenesis.platform.data.api.TokenResponseDto
import com.neogenesis.platform.shared.domain.AuthTokens
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody

interface AuthApi {
    suspend fun register(username: String, password: String, roles: Set<String> = emptySet()): ApiResult<Unit>
    suspend fun login(username: String, password: String): ApiResult<AuthTokens>
    suspend fun logout(refreshToken: String): ApiResult<Unit>
}

class KtorAuthApi(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage? = null,
    private val logger: AppLogger = NoOpLogger
) : AuthApi {
    override suspend fun register(username: String, password: String, roles: Set<String>): ApiResult<Unit> {
        val request = RegisterRequestDto(username, password, roles)
        val correlationId = CorrelationIds.newId()
        return safeApiCall<Unit>(logger, correlationId) {
            client.post("/api/v1/auth/register") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }

    override suspend fun login(username: String, password: String): ApiResult<AuthTokens> {
        val request = LoginRequestDto(username, password)
        val correlationId = CorrelationIds.newId()
        return when (val result = safeApiCall<TokenResponseDto>(logger, correlationId) {
            client.post("/api/v1/auth/login") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }) {
            is ApiResult.Success -> {
                tokenStorage?.writeTokens(result.value.accessToken, result.value.refreshToken)
                ApiResult.Success(AuthTokens(result.value.accessToken, result.value.refreshToken))
            }
            is ApiResult.Failure -> result
        }
    }

    override suspend fun logout(refreshToken: String): ApiResult<Unit> {
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/auth/logout") {
                header("X-Correlation-Id", correlationId)
                setBody(com.neogenesis.platform.data.api.LogoutRequestDto(refreshToken))
            }
        }
    }
}
