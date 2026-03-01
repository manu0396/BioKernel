package com.neogenesis.platform.shared.network

data class NetworkConfig(
    val baseUrl: String,
    val connectTimeoutMs: Long = 10_000,
    val requestTimeoutMs: Long = 15_000,
    val socketTimeoutMs: Long = 15_000,
    val retries: Int = 2,
    val allowCleartext: Boolean = false,
    val tenantId: String? = null,
    val correlationIdProvider: () -> String = { CorrelationIds.newId() }
)

fun allowCleartextForLocalhost(baseUrl: String): Boolean {
    if (!baseUrl.startsWith("http://")) return false
    val host = runCatching { io.ktor.http.Url(baseUrl).host.lowercase() }.getOrDefault("")
    return host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"
}
