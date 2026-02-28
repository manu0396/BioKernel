package com.neogenesis.platform.control.data.oidc

import com.neogenesis.platform.shared.domain.AuthTokens
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.NetworkError
import com.neogenesis.platform.shared.network.TokenStorage
import kotlinx.coroutines.delay

class OidcRepository(
    private val service: OidcDeviceAuthService,
    private val tokenStorage: TokenStorage,
    private val logger: AppLogger
) {
    suspend fun startDeviceAuthorization(config: OidcConfig): ApiResult<DeviceAuthorization> {
        if (config.issuer == "http://mock-auth") {
            return ApiResult.Success(
                DeviceAuthorization(
                    deviceCode = "mock_device_code",
                    userCode = "MOCK-123",
                    verificationUri = "http://mock-auth/verify",
                    verificationUriComplete = "http://mock-auth/verify?code=MOCK-123",
                    expiresIn = 300,
                    intervalSeconds = 1
                )
            )
        }
        
        return when (val response = service.requestDeviceAuthorization(config)) {
            is ApiResult.Success -> {
                ApiResult.Success(
                    DeviceAuthorization(
                        deviceCode = response.value.deviceCode,
                        userCode = response.value.userCode,
                        verificationUri = response.value.verificationUri,
                        verificationUriComplete = response.value.verificationUriComplete,
                        expiresIn = response.value.expiresIn,
                        intervalSeconds = response.value.interval
                    )
                )
            }
            is ApiResult.Failure -> response
        }
    }

    suspend fun pollForTokens(config: OidcConfig, deviceCode: String, intervalSeconds: Int): ApiResult<AuthTokens> {
        if (config.issuer == "http://mock-auth") {
            val mockToken = "mock_access_token_${System.currentTimeMillis()}"
            tokenStorage.writeTokens(mockToken, "mock_refresh_token")
            return ApiResult.Success(AuthTokens(mockToken, "mock_refresh_token"))
        }

        repeat(60) {
            when (val response = service.pollToken(config, deviceCode)) {
                is ApiResult.Success -> {
                    val token = response.value
                    val refresh = token.refreshToken ?: ""
                    tokenStorage.writeTokens(token.accessToken, refresh)
                    return ApiResult.Success(AuthTokens(token.accessToken, refresh))
                }
                is ApiResult.Failure -> {
                    val err = response.error
                    if (err is NetworkError.HttpError && err.status == 400 && err.message.contains("authorization_pending")) {
                        delay(intervalSeconds * 1000L)
                    } else if (err is NetworkError.HttpError && err.status == 400 && err.message.contains("slow_down")) {
                        delay((intervalSeconds + 5) * 1000L)
                    } else {
                        service.logAuthFailure("OIDC device flow failed", err)
                        return response
                    }
                }
            }
        }
        return ApiResult.Failure(NetworkError.TimeoutError("device_code_timeout"))
    }

    fun hasTokens(): Boolean = tokenStorage.readAccessToken()?.isNotBlank() == true

    fun clearTokens() = tokenStorage.clear()
}
