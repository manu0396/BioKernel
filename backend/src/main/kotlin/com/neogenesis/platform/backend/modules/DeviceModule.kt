package com.neogenesis.platform.backend.modules

import com.neogenesis.platform.backend.audit.AuditLogger
import com.neogenesis.platform.backend.http.respondError
import com.neogenesis.platform.backend.security.enforceRole
import com.neogenesis.platform.backend.security.jwtSubject
import com.neogenesis.platform.backend.storage.DevicePairingRepositoryImpl
import com.neogenesis.platform.backend.storage.DeviceRepositoryImpl
import com.neogenesis.platform.backend.storage.SystemIds
import com.neogenesis.platform.shared.domain.*
import com.neogenesis.platform.shared.security.Crypto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

object DeviceModule {
    @Serializable
    data class DeviceRegisterRequest(val serialNumber: String, val firmwareVersion: String)

    @Serializable
    data class PairCompleteRequest(val pairingId: String, val response: String)

    fun register(
        app: Application,
        deviceRepository: DeviceRepositoryImpl,
        pairingRepository: DevicePairingRepositoryImpl,
        pairingSecret: String,
        auditLogger: AuditLogger
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/devices") {
                    post {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        val req = call.receive<DeviceRegisterRequest>()
                        val device = Device(
                            id = DeviceId(UUID.randomUUID().toString()),
                            serialNumber = req.serialNumber,
                            firmwareVersion = req.firmwareVersion,
                            pairedAt = null,
                            active = true
                        )
                        val created = deviceRepository.create(device)
                        val actorId = call.jwtSubject()?.let { UserId(it) }
                            ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(SystemIds.jobId.toString()),
                            actorId = actorId,
                            deviceId = created.id,
                            eventType = "DEVICE_REGISTERED",
                            payload = "{\"deviceId\":\"${created.id.value}\",\"serial\":\"${created.serialNumber}\"}"
                        )
                        call.respond(HttpStatusCode.Created, created)
                    }
                    post("/{id}/pair/start") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        val id = call.parameters["id"] ?: return@post call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_device_id",
                            "Device id required"
                        )
                        val pairing = DevicePairing(
                            id = DevicePairingId(UUID.randomUUID().toString()),
                            deviceId = DeviceId(id),
                            challenge = UUID.randomUUID().toString(),
                            response = null,
                            status = PairingStatus.PENDING,
                            createdAt = Clock.System.now(),
                            completedAt = null
                        )
                        pairingRepository.create(pairing)
                        val actorId = call.jwtSubject()?.let { UserId(it) }
                            ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(SystemIds.jobId.toString()),
                            actorId = actorId,
                            deviceId = DeviceId(id),
                            eventType = "PAIRING_STARTED",
                            payload = "{\"deviceId\":\"$id\",\"pairingId\":\"${pairing.id.value}\"}"
                        )
                        call.respond(mapOf("pairingId" to pairing.id.value, "challenge" to pairing.challenge))
                    }
                    post("/{id}/pair/complete") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        val deviceId = call.parameters["id"] ?: return@post call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_device_id",
                            "Device id required"
                        )
                        val req = call.receive<PairCompleteRequest>()
                        val pairing = pairingRepository.findById(DevicePairingId(req.pairingId))
                            ?: return@post call.respondError(HttpStatusCode.NotFound, "pairing_not_found", "Pairing not found")
                        val expected = Crypto.hmacSha256(pairingSecret, pairing.challenge)
                        val verified = expected == req.response
                        pairingRepository.complete(
                            DevicePairingId(req.pairingId),
                            req.response,
                            if (verified) PairingStatus.VERIFIED else PairingStatus.FAILED,
                            Clock.System.now()
                        )
                        val actorId = call.jwtSubject()?.let { UserId(it) }
                            ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(SystemIds.jobId.toString()),
                            actorId = actorId,
                            deviceId = DeviceId(deviceId),
                            eventType = "PAIRING_COMPLETED",
                            payload = "{\"deviceId\":\"$deviceId\",\"pairingId\":\"${req.pairingId}\",\"verified\":$verified}"
                        )
                        call.respond(mapOf("verified" to verified, "deviceId" to deviceId))
                    }
                }
            }
        }
    }
}
