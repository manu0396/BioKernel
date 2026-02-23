package com.neogenesis.platform.backend.security

import java.util.concurrent.ConcurrentHashMap

class TokenStore {
    private val revokedRefreshTokens = ConcurrentHashMap<String, Long>()

    fun revoke(token: String, expiresAtMs: Long) {
        revokedRefreshTokens[token] = expiresAtMs
    }

    fun isRevoked(token: String): Boolean = revokedRefreshTokens.containsKey(token)
}
