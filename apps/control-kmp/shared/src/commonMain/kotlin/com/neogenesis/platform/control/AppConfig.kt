package com.neogenesis.platform.control

data class AppConfig(
    val httpBaseUrl: String,
    val grpcHost: String,
    val grpcPort: Int,
    val grpcUseTls: Boolean,
    val appVersion: String,
    val oidcIssuer: String,
    val oidcClientId: String,
    val oidcAudience: String? = null,
    val tenantId: String = "tenant-1",
    val traceModeEnabled: Boolean = false,
    val demoModeEnabled: Boolean = false,
    val founderModeEnabled: Boolean = false,
    val commercialModeEnabled: Boolean = false
)
