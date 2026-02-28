package com.neogenesis.platform.shared.security

import kotlin.random.Random

interface CryptoProvider {
    fun sha256(data: String): String
    fun hmacSha256(key: String, data: String): String
    fun generateRandomString(length: Int): String
    fun base64Encode(data: ByteArray): String
}

object Crypto {
    private lateinit var provider: CryptoProvider

    fun init(provider: CryptoProvider) {
        this.provider = provider
    }

    fun generateCodeVerifier(): String = provider.generateRandomString(64)

    fun generateCodeChallenge(verifier: String): String {
        // For PKCE, challenge is SHA256 of verifier, Base64Url encoded
        return provider.sha256(verifier)
    }

    fun hmacSha256(key: String, data: String): String = provider.hmacSha256(key, data)
}
