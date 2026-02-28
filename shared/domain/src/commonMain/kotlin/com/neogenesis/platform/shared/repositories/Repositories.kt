package com.neogenesis.platform.shared.repositories

import com.neogenesis.platform.shared.domain.*
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.datetime.Instant

interface UserRepository {
    suspend fun findByUsername(username: String): User?
    suspend fun findById(id: UserId): User?
    suspend fun create(user: User, passwordHash: String): User
    suspend fun assignRole(userId: UserId, role: Role)
    suspend fun revokeRole(userId: UserId, role: Role)
}

interface DeviceRepository {
    suspend fun findById(id: DeviceId): Device?
    suspend fun recordHealth(deviceId: DeviceId, status: String, at: Instant)
    suspend fun create(device: Device): Device
}

interface DevicePairingRepository {
    suspend fun create(pairing: DevicePairing): DevicePairing
    suspend fun complete(pairingId: DevicePairingId, response: String, status: PairingStatus, completedAt: Instant)
}

interface PrintJobRepository {
    suspend fun create(job: PrintJob, parameters: Map<String, String>): PrintJob
    suspend fun updateStatus(id: PrintJobId, status: PrintJobStatus)
    suspend fun findById(id: PrintJobId): PrintJob?
    suspend fun list(limit: Int = 100): List<PrintJob>
}

interface TelemetryRepository {
    suspend fun append(jobId: PrintJobId, deviceId: DeviceId, frame: TelemetryFrame)
    suspend fun list(jobId: PrintJobId, limit: Int): List<TelemetryFrame>
}

interface EvidenceRepository {
    suspend fun append(log: AuditLog)
    suspend fun list(jobId: PrintJobId): List<AuditLog>
    suspend fun lastHash(jobId: PrintJobId): String?
}

interface BioinkRepository {
    suspend fun createProfile(profile: BioinkProfile): BioinkProfile
    suspend fun createBatch(batch: BioinkBatch): BioinkBatch
    suspend fun listProfiles(): List<BioinkProfile>
    suspend fun listBatches(): List<BioinkBatch>
}

interface RecipeRepository {
    suspend fun create(recipe: Recipe): Recipe
    suspend fun update(recipe: Recipe): Recipe
    suspend fun list(): List<Recipe>
    suspend fun setActive(id: RecipeId, active: Boolean)
    suspend fun findById(id: RecipeId): Recipe?
}
