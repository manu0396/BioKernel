package com.neogenesis.platform.shared.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class HttpClientFactoryTest {
    @Test
    fun allowCleartextOnlyForLocalHosts() {
        assertTrue(allowCleartextForLocalhost("http://localhost:8080"))
        assertTrue(allowCleartextForLocalhost("http://127.0.0.1:8080"))
        assertTrue(allowCleartextForLocalhost("http://10.0.2.2:8080"))
        assertFalse(allowCleartextForLocalhost("http://api.neogenesis.example"))
        assertFalse(allowCleartextForLocalhost("https://localhost:8443"))
    }

    @Test
    fun rejectsCleartextForRemoteHosts() {
        assertFailsWith<IllegalStateException> {
            HttpClientFactory.create(
                NetworkConfig(
                    baseUrl = "http://api.neogenesis.example",
                    allowCleartext = false
                )
            )
        }
    }
}
