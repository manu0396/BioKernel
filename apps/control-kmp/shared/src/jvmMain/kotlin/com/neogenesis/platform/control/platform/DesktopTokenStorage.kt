package com.neogenesis.platform.control.platform

import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.LogLevel
import com.neogenesis.platform.shared.network.TokenStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class DesktopTokenStorage(
    private val logger: AppLogger
) : TokenStorage {
    private val password: String? = System.getenv("REGENOPS_TOKENSTORE_PASSWORD")
    private val storagePath: Path = Paths.get(System.getProperty("user.home"), ".regenops", "tokens.enc")
    private var cache: Pair<String?, String?>? = null

    override fun readAccessToken(): String? {
        ensureLoaded()
        return cache?.first
    }

    override fun readRefreshToken(): String? {
        ensureLoaded()
        return cache?.second
    }

    override fun writeTokens(accessToken: String, refreshToken: String) {
        cache = accessToken to refreshToken
        persist()
    }

    override fun clear() {
        cache = null
        runCatching { Files.deleteIfExists(storagePath) }
    }

    private fun ensureLoaded() {
        if (cache != null) return
        if (!Files.exists(storagePath)) {
            cache = null to null
            return
        }
        val raw = runCatching { Files.readString(storagePath) }.getOrNull() ?: run {
            cache = null to null
            return
        }
        val parts = raw.split("|")
        if (parts.size != 3 || password.isNullOrBlank()) {
            logger.log(LogLevel.WARN, "Desktop token store missing password; tokens not loaded", emptyMap())
            cache = null to null
            return
        }
        val salt = Base64.getDecoder().decode(parts[0])
        val iv = Base64.getDecoder().decode(parts[1])
        val cipherText = Base64.getDecoder().decode(parts[2])
        val decoded = decrypt(password, salt, iv, cipherText)
        val tokens = decoded.split("::", limit = 2)
        cache = tokens.getOrNull(0) to tokens.getOrNull(1)
    }

    private fun persist() {
        val pass = password
        if (pass.isNullOrBlank()) {
            logger.log(LogLevel.WARN, "REGENOPS_TOKENSTORE_PASSWORD missing; tokens kept in memory only", emptyMap())
            return
        }
        val payload = listOf(cache?.first, cache?.second).joinToString("::")
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipherText = encrypt(pass, salt, iv, payload)
        Files.createDirectories(storagePath.parent)
        val encoded = listOf(
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(iv),
            Base64.getEncoder().encodeToString(cipherText)
        ).joinToString("|")
        Files.writeString(storagePath, encoded)
    }

    private fun encrypt(password: String, salt: ByteArray, iv: ByteArray, payload: String): ByteArray {
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(payload.toByteArray())
    }

    private fun decrypt(password: String, salt: ByteArray, iv: ByteArray, cipherText: ByteArray): String {
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return String(cipher.doFinal(cipherText))
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        val key = factory.generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    }
}
