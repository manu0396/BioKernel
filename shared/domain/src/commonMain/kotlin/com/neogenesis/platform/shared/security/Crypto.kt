package com.neogenesis.platform.shared.security

interface CryptoProvider {
    fun sha256(data: String): String
    fun hmacSha256(key: String, data: String): String
    fun generateRandomString(length: Int): String
    fun base64Encode(data: ByteArray): String
}

expect fun platformCryptoProvider(): CryptoProvider
@Suppress("unused")
object Crypto {
    private var overrideProvider: CryptoProvider? = null
    private val provider: CryptoProvider
        get() = overrideProvider ?: platformCryptoProvider()

    fun init(provider: CryptoProvider) {
        overrideProvider = provider
    }

    fun sha256(data: String): String = provider.sha256(data)
    fun hmacSha256(key: String, data: String): String = provider.hmacSha256(key, data)
    fun generateRandomString(length: Int): String = provider.generateRandomString(length)
    fun base64Encode(data: ByteArray): String = provider.base64Encode(data)

    fun generateCodeVerifier(): String = generateRandomString(64)
    fun generateCodeChallenge(verifier: String): String = sha256(verifier)
}