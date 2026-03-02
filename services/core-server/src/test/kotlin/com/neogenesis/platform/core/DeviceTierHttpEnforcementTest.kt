package com.neogenesis.platform.core

import com.neogenesis.platform.core.config.AppConfig
import com.neogenesis.platform.core.modules.AuthModule
import com.neogenesis.platform.shared.domain.Role
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceTierHttpEnforcementTest {
    @Test
    fun tier2DeniedTier3ReadOnlyTier1Allowed() = testApplication {
        configureTestEnv()
        application { module(AppConfig.fromEnv()) }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val token = registerAndLogin(client)

        val tier2Control = client.post("/api/v1/regenops/runs/start") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            deviceHeaders("ANDROID_TABLET", "TIER_2")
            setBody("""{"protocolId":"proto-1","protocolVersion":1}""")
        }
        assertEquals(HttpStatusCode.Forbidden, tier2Control.status)

        val tier3List = client.get("/api/v1/regenops/protocols") {
            header(HttpHeaders.Authorization, "Bearer $token")
            deviceHeaders("TV_DISPLAY", "TIER_3")
        }
        assertEquals(HttpStatusCode.OK, tier3List.status)

        val tier3Metrics = client.get("/api/v1/metrics/drift-alerts") {
            header(HttpHeaders.Authorization, "Bearer $token")
            deviceHeaders("TV_DISPLAY", "TIER_3")
        }
        assertEquals(HttpStatusCode.Forbidden, tier3Metrics.status)

        val tier1Allowed = client.get("/api/v1/regenops/protocols") {
            header(HttpHeaders.Authorization, "Bearer $token")
            deviceHeaders("WINDOWS_DESKTOP", "TIER_1")
        }
        assertEquals(HttpStatusCode.OK, tier1Allowed.status)
    }

    private suspend fun registerAndLogin(client: io.ktor.client.HttpClient): String {
        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.RegisterRequest("operator", "securepass1", setOf(Role.ADMIN)))
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.LoginRequest("operator", "securepass1"))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        return loginResponse.body<AuthModule.TokenResponse>().accessToken
    }

    private fun io.ktor.client.request.HttpRequestBuilder.deviceHeaders(deviceClass: String, tier: String) {
        header("X-Device-Class", deviceClass)
        header("X-Device-Tier", tier)
        header("X-App-Version", "1.0.0")
        header("X-Platform", "desktop")
        header("X-Device-Id", "00000000-0000-0000-0000-000000000002")
    }

    private fun configureTestEnv() {
        val dbName = "test_device_tier_${System.currentTimeMillis()}"
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
        System.setProperty("NG_DEMO_MODE", "true")
    }
}

