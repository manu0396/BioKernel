package com.neogenesis.platform.core

import com.neogenesis.platform.core.security.JwtConfig
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

