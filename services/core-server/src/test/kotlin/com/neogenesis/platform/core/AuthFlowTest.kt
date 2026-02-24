package com.neogenesis.platform.core

import com.neogenesis.platform.core.config.AppConfig
import com.neogenesis.platform.core.modules.AuthModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthFlowTest {
    @Test
    fun loginFlowReturnsTokens() = testApplication {
        configureTestEnv()
        application { module(AppConfig.fromEnv()) }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.RegisterRequest("operator", "securepass1", emptySet()))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.LoginRequest("operator", "securepass1"))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val tokens = loginResponse.body<AuthModule.TokenResponse>()
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
    }

    private fun configureTestEnv() {
        val dbName = "test_auth_${System.currentTimeMillis()}"
        System.setProperty("APP_ENV", "test")
        System.setProperty(
            "DB_URL",
            "jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        )
        System.setProperty("DB_USER", "sa")
        System.setProperty("DB_PASSWORD", "sa")
        System.setProperty("DB_DRIVER", "org.h2.Driver")
        System.setProperty("JWT_ISSUER", "test-issuer")
        System.setProperty("JWT_AUDIENCE", "test-audience")
        System.setProperty("JWT_SECRET", "test-secret")
        System.setProperty("PAIRING_SECRET", "pairing-secret")
        System.setProperty("GRPC_ENABLED", "false")
    }
}

