package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.observability.BusinessMetrics
import com.neogenesis.platform.core.observability.HttpRequestLabels
import com.neogenesis.platform.core.evidence.EvidencePackageBuilder
import com.neogenesis.platform.core.http.respondError
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.core.storage.EvidenceRepositoryImpl
import com.neogenesis.platform.shared.domain.*
import com.neogenesis.platform.shared.evidence.EvidenceExporter
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.core.device.DevicePolicyRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

object EvidenceModule {
    @Serializable
    data class AuditRequest(
        val actorId: String,
        val deviceId: String,
        val eventType: String,
        val payload: String
    )

    fun register(
        app: Application,
        repository: EvidenceRepositoryImpl,
        auditLogger: AuditLogger,
        packageBuilder: EvidencePackageBuilder,
        policyRepository: DevicePolicyRepository
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/evidence") {
                    post("/{jobId}/log") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "AUDITOR"))) return@post
                        if (!call.requireCapability(Capability.ADMIN_SETTINGS, policyRepository)) return@post
                        val jobId = call.parameters["jobId"] ?: return@post call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_job_id",
                            "Missing jobId"
                        )
                        val req = call.receive<AuditRequest>()
                        val log = auditLogger.appendEvent(
                            jobId = PrintJobId(jobId),
                            actorId = UserId(req.actorId),
                            deviceId = DeviceId(req.deviceId),
                            eventType = req.eventType,
                            payload = req.payload
                        )
                        call.respond(HttpStatusCode.Created, log)
                    }
                    get("/{jobId}/export") {
                        try {
                            if (!call.enforceRole(setOf("ADMIN", "AUDITOR"))) return@get
                            if (!call.requireCapability(Capability.QC_REVIEW, policyRepository)) return@get
                            val jobId = call.parameters["jobId"] ?: return@get call.respondError(
                                HttpStatusCode.BadRequest,
                                "missing_job_id",
                                "Missing jobId"
                            )
                            val labels = HttpRequestLabels.fromCall(call)
                            val logs = repository.list(PrintJobId(jobId))
                            val export = EvidenceExporter.export(logs.map {
                                com.neogenesis.platform.shared.evidence.EvidenceEvent(
                                    id = it.id,
                                    timestamp = it.timestamp,
                                    actorId = it.actorId.value,
                                    deviceId = it.deviceId.value,
                                    jobId = it.jobId.value,
                                    eventType = it.eventType,
                                    payloadHash = it.payloadHash,
                                    hash = it.hash,
                                    prevHash = it.prevHash
                                )
                            })
                            BusinessMetrics.evidenceExport(labels, "success")
                            call.respond(export)
                        } catch (err: Throwable) {
                            val labels = HttpRequestLabels.fromCall(call)
                            BusinessMetrics.evidenceExport(labels, "failure")
                            throw err
                        }
                    }
                    get("/{jobId}/package") {
                        try {
                            if (!call.enforceRole(setOf("ADMIN", "AUDITOR"))) return@get
                            if (!call.requireCapability(Capability.QC_REVIEW, policyRepository)) return@get
                            val jobId = call.parameters["jobId"] ?: return@get call.respondError(
                                HttpStatusCode.BadRequest,
                                "missing_job_id",
                                "Missing jobId"
                            )
                            val labels = HttpRequestLabels.fromCall(call)
                            val bundle = packageBuilder.build(PrintJobId(jobId))
                            call.response.headers.append(
                                "Content-Disposition",
                                "attachment; filename=\"${bundle.fileName}\""
                            )
                            BusinessMetrics.evidenceExport(labels, "success")
                            call.respondBytes(bundle.bytes, contentType = ContentType.parse("application/zip"))
                        } catch (err: Throwable) {
                            val labels = HttpRequestLabels.fromCall(call)
                            BusinessMetrics.evidenceExport(labels, "failure")
                            throw err
                        }
                    }
                }
            }
        }
    }
}
