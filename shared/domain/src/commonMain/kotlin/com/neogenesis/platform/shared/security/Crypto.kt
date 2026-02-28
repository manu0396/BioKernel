package com.neogenesis.platform.shared.security

expect object Crypto {
    fun hmacSha256(key: String, data: String): String
    fun sha256(data: String): String
}
