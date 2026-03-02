package com.neogenesis.platform.core

import com.neogenesis.platform.core.config.AppConfig
import com.neogenesis.platform.core.modules.AuthModule
import com.neogenesis.platform.core.modules.BioinkModule
import com.neogenesis.platform.core.modules.DeviceModule
import com.neogenesis.platform.core.modules.EvidenceModule
import com.neogenesis.platform.core.modules.PrintJobModule
import com.neogenesis.platform.shared.domain.BioinkBatch
import com.neogenesis.platform.shared.domain.BioinkProfile
import com.neogenesis.platform.shared.domain.Device
import com.neogenesis.platform.shared.domain.PrintJob
import com.neogenesis.platform.shared.domain.Role
import com.neogenesis.platform.shared.telemetry.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvidenceExportTest {
    @Test
    fun exportEvidencePackageContainsManifest() = testApplication {
        configureTestEnv()
        application { module(AppConfig.fromEnv()) }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val context = createJobWithTelemetry(client)

        client.post("/api/v1/evidence/${context.job.id.value}/log") {
            header(HttpHeaders.Authorization, "Bearer ${context.tokens.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(
                EvidenceModule.AuditRequest(
                    actorId = context.user.id.value,
                    deviceId = context.device.id.value,
                    eventType = "JOB_STARTED",
                    payload = "{\"jobId\":\"${context.job.id.value}\"}"
                )
            )
        }

        val export = client.get("/api/v1/evidence/${context.job.id.value}/package") {
            header(HttpHeaders.Authorization, "Bearer ${context.tokens.accessToken}")
            deviceHeaders()
        }
        assertEquals(HttpStatusCode.OK, export.status)
        val bytes = export.body<ByteArray>()
        val names = mutableSetOf<String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names.add(entry.name)
                entry = zip.nextEntry
            }
        }
        assertTrue(names.contains("manifest.json"))
        assertTrue(names.contains("audit.json"))
    }

    @Test
    fun telemetryExportAliasReturnsCsv() = testApplication {
        configureTestEnv()
        application { module(AppConfig.fromEnv()) }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val context = createJobWithTelemetry(client)

        val export = client.get("/api/v1/telemetry/${context.job.id.value}/export") {
            header(HttpHeaders.Authorization, "Bearer ${context.tokens.accessToken}")
            deviceHeaders()
            header(HttpHeaders.Accept, "text/csv")
        }
        assertEquals(HttpStatusCode.OK, export.status)
        val contentType = export.headers[HttpHeaders.ContentType]
        assertTrue(contentType?.contains("text/csv") == true)
        val payload = export.body<ByteArray>()
        assertTrue(payload.isNotEmpty())
    }

    private data class JobContext(
        val tokens: AuthModule.TokenResponse,
        val user: com.neogenesis.platform.shared.domain.User,
        val device: Device,
        val job: PrintJob
    )

    private suspend fun createJobWithTelemetry(client: io.ktor.client.HttpClient): JobContext {
        val register = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.RegisterRequest("operator", "securepass1", setOf(Role.ADMIN)))
        }
        assertEquals(HttpStatusCode.Created, register.status)
        val user = register.body<com.neogenesis.platform.shared.domain.User>()

        val login = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(AuthModule.LoginRequest("operator", "securepass1"))
        }
        assertEquals(HttpStatusCode.OK, login.status)
        val tokens = login.body<AuthModule.TokenResponse>()

        val device = client.post("/api/v1/devices") {
            header(HttpHeaders.Authorization, "Bearer ${tokens.accessToken}")
            deviceHeaders()
            contentType(ContentType.Application.Json)
            setBody(DeviceModule.DeviceRegisterRequest("serial-001", "1.0.0"))
        }.body<Device>()

        val profile = client.post("/api/v1/bioink/profiles") {
            header(HttpHeaders.Authorization, "Bearer ${tokens.accessToken}")
            deviceHeaders()
            contentType(ContentType.Application.Json)
            setBody(BioinkModule.CreateProfileRequest("Test Ink", "NeoGenesis", "{}"))
        }.body<BioinkProfile>()

        val batch = client.post("/api/v1/bioink/batches") {
            header(HttpHeaders.Authorization, "Bearer ${tokens.accessToken}")
            deviceHeaders()
            contentType(ContentType.Application.Json)
            setBody(BioinkModule.CreateBatchRequest(profile.id.value, "lot-1", "NeoGenesis", Clock.System.now().toEpochMilliseconds()))
        }.body<BioinkBatch>()

        val job = client.post("/api/v1/print-jobs") {
            header(HttpHeaders.Authorization, "Bearer ${tokens.accessToken}")
            deviceHeaders()
            contentType(ContentType.Application.Json)
            setBody(
                PrintJobModule.CreatePrintJobRequest(
                    deviceId = device.id.value,
                    operatorId = user.id.value,
                    bioinkBatchId = batch.id.value,
                    parameters = mapOf("speed" to "1.0")
                )
            )
        }.body<PrintJob>()

        client.post("/api/v1/telemetry/${job.id.value}/${device.id.value}") {
            header(HttpHeaders.Authorization, "Bearer ${tokens.accessToken}")
            deviceHeaders()
            contentType(ContentType.Application.Json)
            setBody(
                TelemetryFrame(
                    timestamp = Clock.System.now(),
                    pressure = PressureReading(120.0),
                    displacement = NozzleDisplacement(1.0),
                    flowRate = FlowRate(5.0),
                    temperature = Temperature(30.0),
                    viscosity = ViscosityEstimation(1.1),
                    pid = PIDState(1.0, 0.1, 0.01),
                    mpc = MPCPrediction(50, 118.0)
                )
            )
        }

        return JobContext(tokens, user, device, job)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.deviceHeaders() {
        header("X-Device-Class", "WINDOWS_DESKTOP")
        header("X-Device-Tier", "TIER_1")
        header("X-App-Version", "1.0.0")
        header("X-Platform", "desktop")
        header("X-Device-Id", "00000000-0000-0000-0000-000000000002")
    }

    private fun configureTestEnv() {
        val dbName = "test_evidence_${System.currentTimeMillis()}"
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

