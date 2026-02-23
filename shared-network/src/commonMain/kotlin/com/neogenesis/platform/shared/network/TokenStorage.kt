package com.neogenesis.platform.shared.network

interface TokenStorage {
    fun readAccessToken(): String?
    fun readRefreshToken(): String?
    fun writeTokens(accessToken: String, refreshToken: String)
    fun clear()
}

class InMemoryTokenStorage : TokenStorage {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override fun readAccessToken(): String? = accessToken

    override fun readRefreshToken(): String? = refreshToken

    override fun writeTokens(accessToken: String, refreshToken: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override fun clear() {
        accessToken = null
        refreshToken = null
    }
}
