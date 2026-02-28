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
                if (!headers.contains("X-Correlation-Id")) {
                    header("X-Correlation-Id", config.correlationIdProvider())
                }
                tokenStorage?.readAccessToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}
