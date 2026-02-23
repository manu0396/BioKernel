package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.LoginRequestDto
import com.neogenesis.platform.data.api.RegisterRequestDto
import com.neogenesis.platform.data.api.TokenResponseDto
import com.neogenesis.platform.shared.domain.AuthTokens
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface AuthApi {
    suspend fun register(username: String, password: String, roles: Set<String> = emptySet()): ApiResult<Unit>
    suspend fun login(username: String, password: String): ApiResult<AuthTokens>
    suspend fun logout(refreshToken: String): ApiResult<Unit>
}

class KtorAuthApi(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage? = null
) : AuthApi {
    override suspend fun register(username: String, password: String, roles: Set<String>): ApiResult<Unit> {
        val request = RegisterRequestDto(username, password, roles)
        return safeApiCall<Unit> {
            client.post("/api/v1/auth/register") {
                setBody(request)
            }
        }
    }

    override suspend fun login(username: String, password: String): ApiResult<AuthTokens> {
        val request = LoginRequestDto(username, password)
        return when (val result = safeApiCall<TokenResponseDto> {
            client.post("/api/v1/auth/login") {
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
        return safeApiCall {
            client.post("/api/v1/auth/logout") {
                setBody(com.neogenesis.platform.data.api.LogoutRequestDto(refreshToken))
            }
        }
    }
}
