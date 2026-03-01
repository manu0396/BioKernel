package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.grpc.TelemetryBus
import com.neogenesis.platform.core.http.respondError
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.storage.TelemetryRepositoryImpl
import com.neogenesis.platform.core.telemetry.TelemetryCheckpointTracker
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.errors.DomainResult
import com.neogenesis.platform.shared.telemetry.TelemetryExport
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import com.neogenesis.platform.shared.validation.TelemetryValidation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

object TelemetryModule {
    fun register(
        app: Application,
        repository: TelemetryRepositoryImpl,
        bus: TelemetryBus,
        checkpointTracker: TelemetryCheckpointTracker
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/telemetry") {
                    post("/{jobId}/{deviceId}") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        val jobId = call.parameters["jobId"] ?: return@post call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_job_id",
                            "Missing jobId"
                        )
                        val deviceId = call.parameters["deviceId"] ?: return@post call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_device_id",
                            "Missing deviceId"
                        )
                        val frame = call.receive<TelemetryFrame>()
                        when (val validated = TelemetryValidation.validate(frame)) {
                            is DomainResult.Success -> {
                                repository.append(PrintJobId(jobId), DeviceId(deviceId), validated.value)
                                bus.emit(validated.value)
                                checkpointTracker.record(PrintJobId(jobId), DeviceId(deviceId))
                                call.respond(HttpStatusCode.Accepted)
                            }
                            is DomainResult.Failure -> call.respondError(
                                HttpStatusCode.UnprocessableEntity,
                                "telemetry_invalid",
                                validated.error.message
                            )
                        }
                    }
                    get("/{jobId}/export") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                        val jobId = call.parameters["jobId"] ?: return@get call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_job_id",
                            "Missing jobId"
                        )
                        val frames = repository.list(PrintJobId(jobId), 10_000)
                        val acceptHeader = call.request.headers[HttpHeaders.Accept] ?: ""
                        if (acceptHeader.contains("text/csv", ignoreCase = true)) {
                            val csv = TelemetryExport.toCsv(frames)
                            call.respondBytes(csv.encodeToByteArray(), contentType = ContentType.Text.CSV)
                        } else {
                            call.respond(
                                mapOf(
                                    "json" to TelemetryExport.toJson(frames),
                                    "csv" to TelemetryExport.toCsv(frames)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

