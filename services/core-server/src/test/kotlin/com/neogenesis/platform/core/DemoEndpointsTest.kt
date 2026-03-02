package com.neogenesis.platform.core

import com.neogenesis.platform.core.config.AppConfig
import com.neogenesis.platform.core.modules.AuthModule
import com.neogenesis.platform.core.modules.DemoSimulatorModule
import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoEndpointsTest {
    @Test
    fun demoMetricsAndCommercialEndpointsReturnData() = testApplication {
        configureTestEnv(demoMode = true)
        application { module(AppConfig.fromEnv()) }

        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        val scoreResponse = client.get("/api/v1/metrics/reproducibility-score") {
            header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
            deviceHeaders()
        }
        assertEquals(HttpStatusCode.OK, scoreResponse.status)

        val alertsResponse = client.get("/api/v1/metrics/drift-alerts") {
            header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
            deviceHeaders()
        }
        assertEquals(HttpStatusCode.OK, alertsResponse.status)

        val pipelineResponse = client.get("/api/v1/commercial/pipeline") {
            header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
            deviceHeaders()
        }
        assertEquals(HttpStatusCode.OK, pipelineResponse.status)

        val exportResponse = client.get("/api/v1/commercial/pipeline/export") {
            header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
            deviceHeaders()
        }
        assertEquals(HttpStatusCode.OK, exportResponse.status)
    }

    @Test
    fun demoSimulatorCreatesRunEvents() = testApplication {
        configureTestEnv(demoMode = true)
        application { module(AppConfig.fromEnv()) }

        val client = createClient { install(ContentNegotiation) { json() } }
        val token = registerAndLogin(client)

        val response =
            client.post("/demo/simulator/runs?tenant_id=tenant-1") {
                header(HttpHeaders.Authorization, "Bearer ${token.accessToken}")
                header("X-Correlation-Id", "corr-test-1")
                deviceHeaders()
                contentType(ContentType.Application.Json)
                setBody(
                    DemoSimulatorModule.SimulatorRunRequest(
                        protocolId = "proto-1",
                        samples = 5,
                        intervalMs = 50
                    )
                )
            }
        assertEquals(HttpStatusCode.Created, response.status)
        val runId = response.body<DemoSimulatorModule.SimulatorRunResponse>().runId
        assertTrue(runId.isNotBlank())

        val event = withTimeout(2_000) { RegenOpsInMemoryStore.events(runId).first() }
        assertEquals(runId, event.runId)
    }

    private suspend fun registerAndLogin(client: io.ktor.client.HttpClient): AuthModule.TokenResponse {
        val register = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.RegisterRequest("operator", "securepass1", setOf(Role.ADMIN)))
        }
        assertEquals(HttpStatusCode.Created, register.status)

        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.LoginRequest("operator", "securepass1"))
        }
        assertEquals(HttpStatusCode.OK, login.status)
        return login.body()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.deviceHeaders() {
        header("X-Device-Class", "WINDOWS_DESKTOP")
        header("X-Device-Tier", "TIER_1")
        header("X-App-Version", "1.0.0")
        header("X-Platform", "desktop")
        header("X-Device-Id", "00000000-0000-0000-0000-000000000002")
    }

    private fun configureTestEnv(demoMode: Boolean) {
        val dbName = "test_demo_${System.currentTimeMillis()}"
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
        System.setProperty("NG_DEMO_MODE", demoMode.toString())
    }
}

