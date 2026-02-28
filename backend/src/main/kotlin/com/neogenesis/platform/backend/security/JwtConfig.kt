package com.neogenesis.platform.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

class JwtConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val validityMs: Long,
    val refreshValidityMs: Long
) {
    private val algorithm = Algorithm.HMAC256(secret)

    fun makeAccessToken(subject: String, role: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(subject)
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + validityMs))
            .sign(algorithm)
    }

    fun makeRefreshToken(subject: String, role: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(subject)
            .withClaim("role", role)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshValidityMs))
            .sign(algorithm)
    }

    fun verifier() = JWT
        .require(Algorithm.HMAC256(secret))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    companion object {
        fun fromEnv(): JwtConfig {
            val issuer = System.getenv("JWT_ISSUER") ?: System.getProperty("JWT_ISSUER")
            val audience = System.getenv("JWT_AUDIENCE") ?: System.getProperty("JWT_AUDIENCE")
            val secret = System.getenv("JWT_SECRET") ?: System.getProperty("JWT_SECRET")
            val accessMs = (System.getenv("JWT_ACCESS_MS") ?: System.getProperty("JWT_ACCESS_MS"))?.toLongOrNull()
            val refreshMs = (System.getenv("JWT_REFRESH_MS") ?: System.getProperty("JWT_REFRESH_MS"))?.toLongOrNull()
            require(!issuer.isNullOrBlank()) { "Missing JWT_ISSUER" }
            require(!audience.isNullOrBlank()) { "Missing JWT_AUDIENCE" }
            require(!secret.isNullOrBlank()) { "Missing JWT_SECRET" }
            return JwtConfig(
                issuer = issuer,
                audience = audience,
                secret = secret,
                validityMs = accessMs ?: 60 * 60 * 1000L,
                refreshValidityMs = refreshMs ?: 7 * 24 * 60 * 60 * 1000L
            )
        }
    }
}
