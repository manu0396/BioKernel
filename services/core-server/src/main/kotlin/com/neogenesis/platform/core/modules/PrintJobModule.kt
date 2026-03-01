package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.grpc.PrintJobEventBus
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.core.security.jwtSubject
import com.neogenesis.platform.core.storage.SystemIds
import com.neogenesis.platform.core.storage.PrintJobRepositoryImpl
import com.neogenesis.platform.proto.v1.PrintJobEvent
import com.neogenesis.platform.shared.domain.*
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.core.device.DevicePolicyRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

object PrintJobModule {
    @Serializable
    data class CreatePrintJobRequest(
        val deviceId: String,
        val operatorId: String,
        val bioinkBatchId: String,
        val parameters: Map<String, String>
    )

    @Serializable
    data class UpdatePrintJobStatusRequest(
        val status: PrintJobStatus
    )

    fun register(
        app: Application,
        repository: PrintJobRepositoryImpl,
        bus: PrintJobEventBus,
        auditLogger: AuditLogger,
        policyRepository: DevicePolicyRepository
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/print-jobs") {
                    get {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                        if (!call.requireCapability(Capability.READ_ONLY_DASHBOARD, policyRepository)) return@get
                        call.respond(repository.list(limit = 200))
                    }
                    post {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        if (!call.requireCapability(Capability.PRINT_CONTROL, policyRepository)) return@post
                        val req = call.receive<CreatePrintJobRequest>()
                        val job = PrintJob(
                            id = PrintJobId(UUID.randomUUID().toString()),
                            deviceId = DeviceId(req.deviceId),
                            operatorId = UserId(req.operatorId),
                            bioinkBatchId = BioinkBatchId(req.bioinkBatchId),
                            createdAt = Clock.System.now(),
                            status = PrintJobStatus.CREATED
                        )
                        val created = repository.create(job, req.parameters)
                        bus.emit(
                            PrintJobEvent.newBuilder()
                                .setJobId(created.id.value)
                                .setEventType("CREATED")
                                .setPayloadJson("{\"jobId\":\"${created.id.value}\"}")
                                .setTimestampMs(Clock.System.now().toEpochMilliseconds())
                                .build()
                        )
                        val actorId = call.jwtSubject()?.let { UserId(it) } ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = created.id,
                            actorId = actorId,
                            deviceId = created.deviceId,
                            eventType = "JOB_CREATED",
                            payload = "{\"jobId\":\"${created.id.value}\",\"status\":\"${created.status}\"}"
                        )
                        call.respond(HttpStatusCode.Created, created)
                    }
                    put("/{id}/status") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@put
                        if (!call.requireCapability(Capability.PRINT_CONTROL, policyRepository)) return@put
                        val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                        val job = repository.findById(PrintJobId(id))
                            ?: return@put call.respond(HttpStatusCode.NotFound)
                        val req = call.receive<UpdatePrintJobStatusRequest>()
                        repository.updateStatus(PrintJobId(id), req.status)
                        val actorId = call.jwtSubject()?.let { UserId(it) } ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(id),
                            actorId = actorId,
                            deviceId = job.deviceId,
                            eventType = "JOB_STATUS_UPDATED",
                            payload = "{\"jobId\":\"$id\",\"status\":\"${req.status}\"}"
                        )
                        bus.emit(
                            PrintJobEvent.newBuilder()
                                .setJobId(id)
                                .setEventType(req.status.name)
                                .setPayloadJson("{\"jobId\":\"$id\",\"status\":\"${req.status}\"}")
                                .setTimestampMs(Clock.System.now().toEpochMilliseconds())
                                .build()
                        )
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}

