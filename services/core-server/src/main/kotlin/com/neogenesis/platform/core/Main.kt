package com.neogenesis.platform.core

import com.neogenesis.platform.core.config.AppConfig
import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.evidence.EvidencePackageBuilder
import com.neogenesis.platform.core.grpc.*
import com.neogenesis.platform.core.http.respondError
import com.neogenesis.platform.core.modules.*
import com.neogenesis.platform.core.observability.NoopMetrics
import com.neogenesis.platform.core.observability.installPrometheusMetrics
import com.neogenesis.platform.core.observability.OpenTelemetryConfig
import com.neogenesis.platform.core.observability.installRequestMetrics
import com.neogenesis.platform.core.security.TokenStore
import com.neogenesis.platform.core.security.configureAuth
import com.neogenesis.platform.core.storage.*
import com.neogenesis.platform.core.telemetry.TelemetryCheckpointTracker
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.core.validation.configureRequestValidation
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.request.header
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID

fun main() {
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.httpPort) {
        module(config)
    }.start(wait = true)
}

fun Application.module(appConfig: AppConfig = AppConfig.fromEnv()) {
    val logger = LoggerFactory.getLogger("Backend")
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
        retrieveFromHeader("X-Correlation-Id")
        replyToHeader("X-Correlation-Id")
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("correlation_id")
        mdc("tenant_id") { call -> call.request.header("X-Tenant-Id") ?: "" }
        mdc("run_id") { call -> call.request.header("X-Run-Id") ?: "" }
    }
    installRequestMetrics(NoopMetrics())
    installPrometheusMetrics()
    OpenTelemetryConfig.init("core-server", System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT"))
    install(ContentNegotiation) { json() }
    configureRequestValidation()
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respondError(
                HttpStatusCode.BadRequest,
                code = "validation_error",
                message = cause.reasons.firstOrNull() ?: "Invalid request"
            )
        }
        exception<SerializationException> { call, _ ->
            call.respondError(HttpStatusCode.BadRequest, code = "invalid_json", message = "Invalid JSON payload")
        }
        exception<JWTVerificationException> { call, _ ->
            call.respondError(HttpStatusCode.Unauthorized, code = "invalid_token", message = "Invalid token")
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled error cid={}", call.callId, cause)
            call.respondError(HttpStatusCode.InternalServerError, code = "internal_error", message = "Internal error")
        }
    }

    val jwtConfig = appConfig.jwt
    configureAuth(jwtConfig)

    DatabaseFactory.init(appConfig.database)
    SystemSeed.ensureSystemEntities()

    val telemetryRepository = TelemetryRepositoryImpl()
    val deviceRepository = DeviceRepositoryImpl()
    val pairingRepository = DevicePairingRepositoryImpl()
    val userRepository = UserRepositoryImpl()
    val evidenceRepository = EvidenceRepositoryImpl()
    val printJobRepository = PrintJobRepositoryImpl()
    val bioinkRepository = BioinkRepositoryImpl()
    val recipeRepository = RecipeRepositoryImpl()

    val telemetryBus = TelemetryBus()
    val commandBus = DeviceCommandBus()
    val printJobEventBus = PrintJobEventBus()
    val tokenStore = TokenStore()
    val auditLogger = AuditLogger(evidenceRepository)
    val checkpointTracker = TelemetryCheckpointTracker(
        auditLogger = auditLogger,
        systemActor = UserId(SystemIds.userId.toString())
    )
    val evidencePackageBuilder = EvidencePackageBuilder(
        config = appConfig,
        evidenceRepository = evidenceRepository,
        telemetryRepository = telemetryRepository
    )

    val firmwareBridge = FirmwareBridgeService(
        telemetryRepository,
        deviceRepository,
        telemetryBus,
        commandBus,
        checkpointTracker
    )
    val platformServices: List<io.grpc.BindableService> = listOf(
        firmwareBridge,
        TelemetryStreamServiceImpl(telemetryBus),
        DeviceControlServiceImpl(commandBus),
        PrintJobEventServiceImpl(printJobEventBus),
        PairingServiceImpl(pairingRepository, appConfig.pairingSecret),
        RegenOpsProtocolService(),
        RegenOpsRunService(),
        RegenOpsGatewayService(),
        RegenOpsMetricsService()
    )
    if (appConfig.grpcEnabled) {
        installGrpcServer(platformServices, port = appConfig.grpcPort)
    }

    AuthModule.register(this, jwtConfig, userRepository, tokenStore, auditLogger)
    UserModule.register(this, userRepository)
    DeviceModule.register(this, deviceRepository, pairingRepository, appConfig.pairingSecret, auditLogger)
    TelemetryModule.register(this, telemetryRepository, telemetryBus, checkpointTracker)
    PrintJobModule.register(this, printJobRepository, printJobEventBus, auditLogger)
    EvidenceModule.register(this, evidenceRepository, auditLogger, evidencePackageBuilder)
    DigitalTwinModule.register(this)
    HospitalIntegrationModule.register(this)
    AdminOpsModule.register(this)
    BioinkModule.register(this, bioinkRepository)
    RecipeModule.register(this, recipeRepository, auditLogger)
    HealthModule.register(this)
    RegenOpsHttpModule.register(this)
}

