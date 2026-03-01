package com.neogenesis.platform.core.config

import com.neogenesis.platform.core.security.JwtConfig

data class AppConfig(
    val environment: String,
    val httpPort: Int,
    val grpcPort: Int,
    val grpcEnabled: Boolean,
    val grpcTls: GrpcTlsConfig,
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
            val grpcTls = GrpcTlsConfig.fromEnv()
            val jwt = JwtConfig.fromEnv()
            val database = DatabaseConfig.fromEnv()
            val pairingSecret = Env.required("PAIRING_SECRET")
            return AppConfig(
                environment = environment,
                httpPort = httpPort,
                grpcPort = grpcPort,
                grpcEnabled = grpcEnabled,
                grpcTls = grpcTls,
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

data class GrpcTlsConfig(
    val enabled: Boolean,
    val certChainPath: String?,
    val privateKeyPath: String?,
    val trustCertPath: String?,
    val requireClientAuth: Boolean,
    val reloadIntervalSeconds: Long
) {
    companion object {
        fun fromEnv(): GrpcTlsConfig {
            val enabled = Env.bool("GRPC_TLS_ENABLED", false)
            val certChainPath = Env.get("GRPC_TLS_CERT_CHAIN_PATH")
            val privateKeyPath = Env.get("GRPC_TLS_PRIVATE_KEY_PATH")
            val trustCertPath = Env.get("GRPC_TLS_TRUST_CERT_PATH")
            val requireClientAuth = Env.bool("GRPC_TLS_REQUIRE_CLIENT_AUTH", true)
            val reloadIntervalSeconds = Env.int("GRPC_TLS_RELOAD_INTERVAL_SEC", 300).toLong()
            val resolvedEnabled = enabled || (!certChainPath.isNullOrBlank() && !privateKeyPath.isNullOrBlank())
            return GrpcTlsConfig(
                enabled = resolvedEnabled,
                certChainPath = certChainPath,
                privateKeyPath = privateKeyPath,
                trustCertPath = trustCertPath,
                requireClientAuth = requireClientAuth,
                reloadIntervalSeconds = reloadIntervalSeconds
            )
        }
    }
}
