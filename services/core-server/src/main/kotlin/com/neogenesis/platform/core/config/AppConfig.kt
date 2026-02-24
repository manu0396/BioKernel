package com.neogenesis.platform.core.config

import com.neogenesis.platform.core.security.JwtConfig

data class AppConfig(
    val environment: String,
    val httpPort: Int,
    val grpcPort: Int,
    val grpcEnabled: Boolean,
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val pairingSecret: String
) {
    companion object {
        fun fromEnv(): AppConfig {
            val environment = Env.get("APP_ENV") ?: "dev"
            val httpPort = Env.int("HTTP_PORT", 8080)
            val grpcPort = Env.int("GRPC_PORT", 9090)
            val grpcEnabled = Env.bool("GRPC_ENABLED", true)
            val jwt = JwtConfig.fromEnv()
            val database = DatabaseConfig.fromEnv()
            val pairingSecret = Env.required("PAIRING_SECRET")
            return AppConfig(
                environment = environment,
                httpPort = httpPort,
                grpcPort = grpcPort,
                grpcEnabled = grpcEnabled,
                jwt = jwt,
                database = database,
                pairingSecret = pairingSecret
            )
        }
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String?,
    val maxPoolSize: Int
) {
    companion object {
        fun fromEnv(): DatabaseConfig {
            return DatabaseConfig(
                url = Env.required("DB_URL"),
                user = Env.required("DB_USER"),
                password = Env.required("DB_PASSWORD"),
                driver = Env.get("DB_DRIVER"),
                maxPoolSize = Env.int("DB_POOL_SIZE", 10)
            )
        }
    }
}

object Env {
    fun get(key: String): String? = System.getenv(key) ?: System.getProperty(key)

    fun required(key: String): String =
        get(key) ?: error("Missing required environment variable: $key")

    fun int(key: String, defaultValue: Int): Int {
        val value = get(key) ?: return defaultValue
        return value.toIntOrNull() ?: error("Invalid integer for $key")
    }

    fun bool(key: String, defaultValue: Boolean): Boolean {
        val value = get(key) ?: return defaultValue
        return value.equals("true", ignoreCase = true) || value == "1"
    }
}

