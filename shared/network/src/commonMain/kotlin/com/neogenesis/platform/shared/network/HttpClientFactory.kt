package com.neogenesis.platform.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(config: NetworkConfig, tokenStorage: TokenStorage? = null): HttpClient {
        if (!config.allowCleartext && config.baseUrl.startsWith("http://")) {
            error("Cleartext HTTP is disabled for baseUrl=${config.baseUrl}")
        }
        return HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = config.connectTimeoutMs
                requestTimeoutMillis = config.requestTimeoutMs
                socketTimeoutMillis = config.socketTimeoutMs
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = config.retries)
                exponentialDelay()
            }
            defaultRequest {
                url.takeFrom(config.baseUrl)
                val tenantId = config.tenantId?.takeIf { it.isNotBlank() }
                if (tenantId != null && !url.parameters.contains("tenant_id")) {
                    url.parameters.append("tenant_id", tenantId)
                }
                if (tenantId != null && !headers.contains("X-Tenant-Id")) {
                    header("X-Tenant-Id", tenantId)
                }
                if (!headers.contains("X-Correlation-Id")) {
                    header("X-Correlation-Id", config.correlationIdProvider())
                }
                val deviceInfo = config.deviceInfoProvider?.invoke()
                if (deviceInfo != null) {
                    if (!headers.contains(DeviceHeaders.DEVICE_ID) && deviceInfo.deviceId != null) {
                        header(DeviceHeaders.DEVICE_ID, deviceInfo.deviceId)
                    }
                    if (!headers.contains(DeviceHeaders.DEVICE_CLASS)) {
                        header(DeviceHeaders.DEVICE_CLASS, deviceInfo.deviceClass.name)
                    }
                    if (!headers.contains(DeviceHeaders.DEVICE_TIER)) {
                        header(DeviceHeaders.DEVICE_TIER, deviceInfo.tier.name)
                    }
                    if (!headers.contains(DeviceHeaders.APP_VERSION)) {
                        header(DeviceHeaders.APP_VERSION, deviceInfo.appVersion)
                    }
                    if (!headers.contains(DeviceHeaders.PLATFORM)) {
                        header(DeviceHeaders.PLATFORM, deviceInfo.platform)
                    }
                    if (!headers.contains(DeviceHeaders.OS_VERSION) && deviceInfo.osVersion != null) {
                        header(DeviceHeaders.OS_VERSION, deviceInfo.osVersion)
                    }
                    if (!headers.contains(DeviceHeaders.DEVICE_MODEL) && deviceInfo.model != null) {
                        header(DeviceHeaders.DEVICE_MODEL, deviceInfo.model)
                    }
                    if (!headers.contains(DeviceHeaders.POLICY_VERSION) && deviceInfo.policyVersion != null) {
                        header(DeviceHeaders.POLICY_VERSION, deviceInfo.policyVersion.toString())
                    }
                }
                tokenStorage?.readAccessToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}
