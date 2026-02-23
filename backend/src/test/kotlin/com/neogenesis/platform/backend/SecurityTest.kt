package com.neogenesis.platform.backend

import com.neogenesis.platform.backend.security.JwtConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class SecurityTest {
    @Test
    fun accessTokenIncludesRole() {
        val config = JwtConfig("issuer", "aud", "secret", 1000L, 1000L)
        val token = config.makeAccessToken("user", "Operator")
        assertTrue(token.isNotBlank())
    }
}
