package com.neogenesis.platform.backend

import com.neogenesis.platform.backend.config.AppConfig
import com.neogenesis.platform.backend.audit.AuditLogger
import com.neogenesis.platform.backend.evidence.EvidencePackageBuilder
import com.neogenesis.platform.backend.grpc.*
import com.neogenesis.platform.backend.http.respondError
import com.neogenesis.platform.backend.modules.*
import com.neogenesis.platform.backend.observability.NoopMetrics
import com.neogenesis.platform.backend.observability.installRequestMetrics
import com.neogenesis.platform.backend.security.TokenStore
import com.neogenesis.platform.backend.security.configureAuth
import com.neogenesis.platform.backend.storage.*
import com.neogenesis.platform.backend.telemetry.TelemetryCheckpointTracker
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.backend.validation.configureRequestValidation
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
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
        retrieveFromHeader(HttpHeaders.XRequestId)
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("cid")
    }
    installRequestMetrics(NoopMetrics())
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
    val platformServices = listOf(
        firmwareBridge,
        TelemetryStreamServiceImpl(telemetryBus),
        DeviceControlServiceImpl(commandBus),
        PrintJobEventServiceImpl(printJobEventBus),
        PairingServiceImpl(pairingRepository, appConfig.pairingSecret)
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
}
