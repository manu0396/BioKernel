package com.neogenesis.platform.desktop.storage

import com.neogenesis.platform.shared.network.TokenStorage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DesktopTokenStorage(
    private val path: Path = defaultPath(),
    private val keyPath: Path = defaultKeyPath()
) : TokenStorage {
    override fun readAccessToken(): String? = readTokens()?.first

    override fun readRefreshToken(): String? = readTokens()?.second

    override fun writeTokens(accessToken: String, refreshToken: String) {
        ensureDir()
        val payload = "$accessToken\n$refreshToken".encodeToByteArray()
        val key = resolveKey()
        val encrypted = encrypt(payload, key)
        Files.writeString(
            path,
            Base64.getEncoder().encodeToString(encrypted),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
        setOwnerOnlyPermissions(path)
    }

    override fun clear() {
        if (Files.exists(path)) {
            Files.delete(path)
        }
    }

    private fun readTokens(): Pair<String, String>? {
        if (!Files.exists(path)) return null
        val encoded = Files.readString(path).trim()
        if (encoded.isBlank()) return null
        return runCatching {
            val decoded = Base64.getDecoder().decode(encoded)
            val plaintext = decrypt(decoded, resolveKey())
            val parts = plaintext.decodeToString().split('\n', limit = 2)
            if (parts.size != 2) return null
            parts[0] to parts[1]
        }.getOrNull()
    }

    private fun resolveKey(): ByteArray {
        val envKey = System.getenv("NEOGENESIS_DESKTOP_TOKEN_KEY")?.trim().orEmpty()
        if (envKey.isNotEmpty()) {
            val fromBase64 = runCatching { Base64.getDecoder().decode(envKey) }.getOrNull()
            val material = fromBase64 ?: envKey.encodeToByteArray()
            return normalizeKey(material)
        }

        ensureDir()
        if (!Files.exists(keyPath)) {
            val random = ByteArray(KEY_SIZE).also { secureRandom.nextBytes(it) }
            Files.write(keyPath, random, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            setOwnerOnlyPermissions(keyPath)
        }
        return normalizeKey(Files.readAllBytes(keyPath))
    }

    private fun encrypt(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
        val ciphertext = cipher.doFinal(data)
        return iv + ciphertext
    }

    private fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
        require(data.size > IV_SIZE) { "Invalid token payload" }
        val iv = data.copyOfRange(0, IV_SIZE)
        val body = data.copyOfRange(IV_SIZE, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(body)
    }

    private fun normalizeKey(raw: ByteArray): ByteArray =
        if (raw.size == KEY_SIZE) raw else raw.copyOf(KEY_SIZE)

    private fun ensureDir() {
        Files.createDirectories(path.parent)
    }

    private fun setOwnerOnlyPermissions(target: Path) {
        runCatching {
            val supported = target.fileSystem.supportedFileAttributeViews()
            if ("posix" in supported) {
                Files.setPosixFilePermissions(
                    target,
                    PosixFilePermissions.fromString("rw-------")
                )
            } else if ("acl" !in supported) {
                Files.setAttribute(target, "dos:readonly", false)
            }
        }
    }

    private companion object {
        private const val IV_SIZE = 12
        private const val KEY_SIZE = 32
        private const val TAG_SIZE_BITS = 128
        private val secureRandom = SecureRandom()

        fun defaultPath(): Path {
            val home = System.getProperty("user.home") ?: "."
            return Paths.get(home, ".neogenesis", "tokens.sec")
        }

        fun defaultKeyPath(): Path {
            val home = System.getProperty("user.home") ?: "."
            return Paths.get(home, ".neogenesis", "tokens.key")
        }
    }
}
