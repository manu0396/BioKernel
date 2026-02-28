package com.neogenesis.platform.control

data class AppConfig(
    val httpBaseUrl: String,
    val grpcHost: String,
    val grpcPort: Int,
    val grpcUseTls: Boolean,
    val oidcIssuer: String,
    val oidcClientId: String,
    val oidcAudience: String? = null,
    val traceModeEnabled: Boolean = false,
    val demoModeEnabled: Boolean = false,
    val founderModeEnabled: Boolean = false,
    val commercialModeEnabled: Boolean = false
)
