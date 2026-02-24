package com.neogenesis.platform.control.data.oidc

import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.LogLevel
import com.neogenesis.platform.shared.network.NetworkError
import com.neogenesis.platform.shared.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.contentType
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.content.FormDataContent

class OidcDeviceAuthService(
    private val client: HttpClient,
    private val logger: AppLogger
) {
    suspend fun discover(issuer: String): ApiResult<OidcDiscoveryResponse> {
        val target = issuer.trimEnd('/') + "/.well-known/openid-configuration"
        return safeApiCall(logger, correlationId = null) {
            client.get(target)
        }
    }

    suspend fun requestDeviceAuthorization(config: OidcConfig): ApiResult<OidcDeviceAuthorizationResponse> {
        val discovery = discover(config.issuer)
        if (discovery is ApiResult.Failure) return discovery
        val endpoint = (discovery as ApiResult.Success).value.deviceAuthorizationEndpoint
        val params = Parameters.build {
            append("client_id", config.clientId)
            config.audience?.takeIf { it.isNotBlank() }?.let { append("audience", it) }
            append("scope", "openid profile offline_access")
        }
        return safeApiCall(logger, correlationId = null) {
            client.post(endpoint) {
                setBody(FormDataContent(params))
                contentType(ContentType.Application.FormUrlEncoded)
            }
        }
    }

    suspend fun pollToken(config: OidcConfig, deviceCode: String): ApiResult<OidcTokenResponse> {
        val discovery = discover(config.issuer)
        if (discovery is ApiResult.Failure) return discovery
        val endpoint = (discovery as ApiResult.Success).value.tokenEndpoint
        val params = Parameters.build {
            append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            append("device_code", deviceCode)
            append("client_id", config.clientId)
        }
        return safeApiCall(logger, correlationId = null) {
            client.post(endpoint) {
                setBody(FormDataContent(params))
                contentType(ContentType.Application.FormUrlEncoded)
            }
        }
    }

    fun logAuthFailure(message: String, error: NetworkError) {
        logger.log(LogLevel.WARN, message, mapOf("reason" to error.message))
    }
}
