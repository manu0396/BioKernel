package com.neogenesis.platform.shared.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun platformCryptoProvider(): CryptoProvider = JvmSharedCryptoProvider

internal object JvmSharedCryptoProvider : CryptoProvider {
    private val secureRandom = SecureRandom()

    override fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun hmacSha256(key: String, data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    override fun generateRandomString(length: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
        return buildString(length) { repeat(length) { append(alphabet[secureRandom.nextInt(alphabet.length)]) } }
    }

    override fun base64Encode(data: ByteArray): String =
        Base64.getEncoder().withoutPadding().encodeToString(data)
}