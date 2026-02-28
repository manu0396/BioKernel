package com.neogenesis.platform.shared.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserId(val value: String)

@Serializable
data class DeviceId(val value: String)

@Serializable
data class PrintJobId(val value: String)

@Serializable
data class BioinkProfileId(val value: String)

@Serializable
data class BioinkBatchId(val value: String)

@Serializable
data class RecipeId(val value: String)

@Serializable
data class FirmwareVersionId(val value: String)

@Serializable
data class DevicePairingId(val value: String)

@Serializable
data class ProtocolId(val value: String)

@Serializable
data class ProtocolVersionId(val value: String)

@Serializable
data class RunId(val value: String)

@Serializable
enum class Role {
    ADMIN,
    OPERATOR,
    RESEARCHER,
    AUDITOR
}

@Serializable
data class User(
    val id: UserId,
    val username: String,
    val roles: Set<Role>,
    val active: Boolean,
    val createdAt: Instant
)

@Serializable
data class RoleAssignment(
    val userId: UserId,
    val role: Role
)

@Serializable
data class Device(
    val id: DeviceId,
    val serialNumber: String,
    val firmwareVersion: String,
    val pairedAt: Instant?,
    val active: Boolean
)

@Serializable
data class DevicePairing(
    val id: DevicePairingId,
    val deviceId: DeviceId,
    val challenge: String,
    val response: String?,
    val status: PairingStatus,
    val createdAt: Instant,
    val completedAt: Instant?
)

@Serializable
enum class PairingStatus {
    PENDING,
    VERIFIED,
    FAILED
}

@Serializable
data class DeviceHealthStatus(
    val id: String,
    val deviceId: DeviceId,
    val status: String,
    val details: String?,
    val createdAt: Instant
)

@Serializable
data class FirmwareVersion(
    val id: FirmwareVersionId,
    val version: String,
    val signedHash: String,
    val createdAt: Instant
)

@Serializable
data class BioinkProfile(
    val id: BioinkProfileId,
    val name: String,
    val manufacturer: String?,
    val viscosityModel: String,
    val createdAt: Instant
)

@Serializable
data class BioinkBatch(
    val id: BioinkBatchId,
    val profileId: BioinkProfileId,
    val lotNumber: String,
    val manufacturer: String,
    val createdAt: Instant,
    val expiresAt: Instant
)

@Serializable
data class Recipe(
    val id: RecipeId,
    val name: String,
    val description: String?,
    val parameters: Map<String, String>,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class Protocol(
    val id: ProtocolId,
    val name: String,
    val summary: String,
    val latestVersion: ProtocolVersion?,
    val versions: List<ProtocolVersion> = emptyList()
)

@Serializable
data class ProtocolVersion(
    val id: ProtocolVersionId,
    val protocolId: ProtocolId,
    val version: String,
    val createdAt: Instant,
    val author: String,
    val payload: String,
    val published: Boolean
)

@Serializable
data class PrintJob(
    val id: PrintJobId,
    val deviceId: DeviceId,
    val operatorId: UserId,
    val bioinkBatchId: BioinkBatchId,
    val createdAt: Instant,
    val status: PrintJobStatus
)

@Serializable
data class PrintJobParameters(
    val id: String,
    val jobId: PrintJobId,
    val parameters: Map<String, String>
)

@Serializable
data class PrintJobEvent(
    val id: String,
    val jobId: PrintJobId,
    val eventType: String,
    val payloadJson: String,
    val createdAt: Instant
)

@Serializable
enum class PrintJobStatus {
    CREATED,
    RUNNING,
    PAUSED,
    COMPLETED,
    ABORTED
}

@Serializable
data class Run(
    val id: RunId,
    val protocolId: ProtocolId,
    val protocolVersionId: ProtocolVersionId,
    val status: RunStatus,
    val createdAt: Instant,
    val updatedAt: Instant?
)

@Serializable
enum class RunStatus {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    ABORTED,
    FAILED
}

@Serializable
data class RunEvent(
    val id: String,
    val runId: RunId,
    val eventType: String,
    val message: String,
    val createdAt: Instant
)

@Serializable
data class TelemetryRecord(
    val id: String,
    val jobId: PrintJobId,
    val deviceId: DeviceId,
    val timestamp: Instant,
    val frame: com.neogenesis.platform.shared.telemetry.TelemetryFrame
)

@Serializable
data class DigitalTwinMetric(
    val id: String,
    val jobId: PrintJobId,
    val timestamp: Instant,
    val deviation: com.neogenesis.platform.shared.digitaltwin.DeviationMetrics
)

@Serializable
data class AuditLog(
    val id: String,
    val jobId: PrintJobId,
    val actorId: UserId,
    val deviceId: DeviceId,
    val eventType: String,
    val payloadHash: String,
    val hash: String,
    val prevHash: String?,
    val timestamp: Instant
)

@Serializable
data class EvidencePackage(
    val jobId: PrintJobId,
    val manifestJson: String,
    val dataJson: String,
    val hash: String,
    val createdAt: Instant
)
