package com.neogenesis.platform.core

import com.neogenesis.platform.core.config.AppConfig
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthRouteTest {
    @Test
    fun healthEndpointReturnsOk() = testApplication {
        configureTestEnv()
        application { module(AppConfig.fromEnv()) }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    private fun configureTestEnv() {
        val dbName = "test_health_${System.currentTimeMillis()}"
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

