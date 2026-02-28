package com.neogenesis.platform.shared.config

interface SecureConfig {
    fun get(key: String): String
}

class SecureConfigProvider(private val values: Map<String, String>) : SecureConfig {
    override fun get(key: String): String = values[key] ?: error("Missing config: $key")
}
