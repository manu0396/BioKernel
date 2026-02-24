package com.neogenesis.platform.core.storage

import com.neogenesis.platform.shared.domain.*
import com.neogenesis.platform.shared.repositories.*
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override suspend fun findByUsername(username: String): User? = transaction {
        Users.select { Users.username eq username }
            .map { toUser(it) }
            .singleOrNull()
    }

    override suspend fun findById(id: UserId): User? = transaction {
        Users.select { Users.id eq UUID.fromString(id.value) }
            .map { toUser(it) }
            .singleOrNull()
    }

    override suspend fun create(user: User, passwordHash: String): User = transaction {
        Users.insert {
            it[id] = UUID.fromString(user.id.value)
            it[username] = user.username
            it[Users.passwordHash] = passwordHash
            it[active] = user.active
            it[createdAt] = user.createdAt.toEpochMilliseconds()
        }
        user
    }

    override suspend fun assignRole(userId: UserId, role: Role) {
        transaction {
            val roleId = ensureRole(role)
            UserRoles.insertIgnore {
                it[UserRoles.userId] = EntityID(UUID.fromString(userId.value), Users)
                it[UserRoles.roleId] = roleId
            }
        }
    }

    override suspend fun revokeRole(userId: UserId, role: Role) {
        transaction {
            val roleId = ensureRole(role)
            val userEntityId = EntityID(UUID.fromString(userId.value), Users)
            UserRoles.deleteWhere {
                (UserRoles.userId.eq(userEntityId)) and (UserRoles.roleId.eq(roleId))
            }
        }
    }

    fun findPasswordHash(username: String): String? = transaction {
        Users.select { Users.username eq username }
            .map { it[Users.passwordHash] }
            .singleOrNull()
    }

    fun rolesForUser(userId: UserId): Set<Role> = transaction {
        (UserRoles innerJoin Roles).select { UserRoles.userId eq EntityID(UUID.fromString(userId.value), Users) }
            .map { Role.valueOf(it[Roles.name]) }
            .toSet()
    }

    private fun toUser(row: ResultRow): User {
        val id = UserId(row[Users.id].value.toString())
        val roles = rolesForUser(id)
        return User(
            id = id,
            username = row[Users.username],
            roles = if (roles.isEmpty()) setOf(Role.OPERATOR) else roles,
            active = row[Users.active],
            createdAt = Instant.fromEpochMilliseconds(row[Users.createdAt])
        )
    }

    private fun ensureRole(role: Role): EntityID<UUID> {
        val existing = Roles.select { Roles.name eq role.name }.singleOrNull()
        return if (existing != null) {
            existing[Roles.id]
        } else {
            val id = UUID.randomUUID()
            Roles.insert {
                it[Roles.id] = id
                it[name] = role.name
            }
            EntityID(id, Roles)
        }
    }
}

class DeviceRepositoryImpl : DeviceRepository {
    override suspend fun findById(id: DeviceId): Device? = transaction {
        Devices.select { Devices.id eq UUID.fromString(id.value) }
            .map {
                Device(
                    id = DeviceId(it[Devices.id].value.toString()),
                    serialNumber = it[Devices.serialNumber],
                    firmwareVersion = it[Devices.firmwareVersion],
                    pairedAt = it[Devices.pairedAt]?.let { ms -> Instant.fromEpochMilliseconds(ms) },
                    active = it[Devices.active]
                )
            }
            .singleOrNull()
    }

    override suspend fun recordHealth(deviceId: DeviceId, status: String, at: Instant) {
        transaction {
            DeviceHealth.insert {
                it[id] = UUID.randomUUID()
                it[DeviceHealth.deviceId] = UUID.fromString(deviceId.value)
                it[DeviceHealth.status] = status
                it[DeviceHealth.details] = null
                it[DeviceHealth.createdAt] = at.toEpochMilliseconds()
            }
        }
    }

    override suspend fun create(device: Device): Device {
        transaction {
            Devices.insert {
                it[id] = UUID.fromString(device.id.value)
                it[serialNumber] = device.serialNumber
                it[firmwareVersion] = device.firmwareVersion
                it[pairedAt] = device.pairedAt?.toEpochMilliseconds()
                it[active] = device.active
            }
        }
        return device
    }
}

class DevicePairingRepositoryImpl : DevicePairingRepository {
    override suspend fun create(pairing: DevicePairing): DevicePairing {
        transaction {
            DevicePairings.insert {
                it[id] = UUID.fromString(pairing.id.value)
                it[deviceId] = UUID.fromString(pairing.deviceId.value)
                it[challenge] = pairing.challenge
                it[response] = pairing.response
                it[status] = pairing.status.name
                it[createdAt] = pairing.createdAt.toEpochMilliseconds()
                it[completedAt] = pairing.completedAt?.toEpochMilliseconds()
            }
        }
        return pairing
    }

    override suspend fun complete(pairingId: DevicePairingId, response: String, status: PairingStatus, completedAt: Instant) {
        transaction {
            DevicePairings.update({ DevicePairings.id eq UUID.fromString(pairingId.value) }) {
                it[DevicePairings.response] = response
                it[DevicePairings.status] = status.name
                it[DevicePairings.completedAt] = completedAt.toEpochMilliseconds()
            }
        }
    }

    fun findById(id: DevicePairingId): DevicePairing? = transaction {
        DevicePairings.select { DevicePairings.id eq UUID.fromString(id.value) }
            .map {
                DevicePairing(
                    id = DevicePairingId(it[DevicePairings.id].value.toString()),
                    deviceId = DeviceId(it[DevicePairings.deviceId].value.toString()),
                    challenge = it[DevicePairings.challenge],
                    response = it[DevicePairings.response],
                    status = PairingStatus.valueOf(it[DevicePairings.status]),
                    createdAt = Instant.fromEpochMilliseconds(it[DevicePairings.createdAt]),
                    completedAt = it[DevicePairings.completedAt]?.let { ms -> Instant.fromEpochMilliseconds(ms) }
                )
            }
            .singleOrNull()
    }
}

class PrintJobRepositoryImpl : PrintJobRepository {
    private val json = Json { encodeDefaults = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    override suspend fun create(job: PrintJob, parameters: Map<String, String>): PrintJob = transaction {
        PrintJobs.insert {
            it[id] = UUID.fromString(job.id.value)
            it[deviceId] = UUID.fromString(job.deviceId.value)
            it[operatorId] = UUID.fromString(job.operatorId.value)
            it[bioinkBatchId] = UUID.fromString(job.bioinkBatchId.value)
            it[createdAt] = job.createdAt.toEpochMilliseconds()
            it[status] = job.status.name
        }
        PrintJobParameters.insert {
            it[id] = UUID.randomUUID()
            it[jobId] = UUID.fromString(job.id.value)
            it[parametersJson] = json.encodeToString(mapSerializer, parameters)
        }
        job
    }

    override suspend fun updateStatus(id: PrintJobId, status: PrintJobStatus) {
        transaction {
            PrintJobs.update({ PrintJobs.id eq UUID.fromString(id.value) }) {
                it[PrintJobs.status] = status.name
            }
        }
    }

    override suspend fun findById(id: PrintJobId): PrintJob? = transaction {
        val job = PrintJobs.select { PrintJobs.id eq UUID.fromString(id.value) }.singleOrNull() ?: return@transaction null
        PrintJob(
            id = PrintJobId(job[PrintJobs.id].value.toString()),
            deviceId = DeviceId(job[PrintJobs.deviceId].value.toString()),
            operatorId = UserId(job[PrintJobs.operatorId].value.toString()),
            bioinkBatchId = BioinkBatchId(job[PrintJobs.bioinkBatchId].value.toString()),
            createdAt = Instant.fromEpochMilliseconds(job[PrintJobs.createdAt]),
            status = PrintJobStatus.valueOf(job[PrintJobs.status])
        )
    }

    override suspend fun list(limit: Int): List<PrintJob> = transaction {
        PrintJobs.selectAll()
            .orderBy(PrintJobs.createdAt, SortOrder.DESC)
            .limit(limit)
            .map {
                PrintJob(
                    id = PrintJobId(it[PrintJobs.id].value.toString()),
                    deviceId = DeviceId(it[PrintJobs.deviceId].value.toString()),
                    operatorId = UserId(it[PrintJobs.operatorId].value.toString()),
                    bioinkBatchId = BioinkBatchId(it[PrintJobs.bioinkBatchId].value.toString()),
                    createdAt = Instant.fromEpochMilliseconds(it[PrintJobs.createdAt]),
                    status = PrintJobStatus.valueOf(it[PrintJobs.status])
                )
            }
    }
}

class TelemetryRepositoryImpl : TelemetryRepository {
    private val json = Json { encodeDefaults = true }

    override suspend fun append(jobId: PrintJobId, deviceId: DeviceId, frame: TelemetryFrame) {
        transaction {
            TelemetryRecords.insert {
                it[id] = UUID.randomUUID()
                it[TelemetryRecords.jobId] = UUID.fromString(jobId.value)
                it[TelemetryRecords.deviceId] = UUID.fromString(deviceId.value)
                it[timestamp] = frame.timestamp.toEpochMilliseconds()
                it[payloadJson] = json.encodeToString(TelemetryFrame.serializer(), frame)
            }
        }
    }

    override suspend fun list(jobId: PrintJobId, limit: Int): List<TelemetryFrame> = transaction {
        TelemetryRecords.select { TelemetryRecords.jobId eq UUID.fromString(jobId.value) }
            .orderBy(TelemetryRecords.timestamp, SortOrder.ASC)
            .limit(limit)
            .map { json.decodeFromString(TelemetryFrame.serializer(), it[TelemetryRecords.payloadJson]) }
    }

    fun store(frame: com.neogenesis.platform.firmware.v1.TelemetryFrame) {
        val mapped = com.neogenesis.platform.core.grpc.TelemetryMapper.fromFirmware(frame)
        transaction {
            TelemetryRecords.insert {
                it[id] = UUID.randomUUID()
                it[TelemetryRecords.jobId] = UUID.fromString(frame.jobId)
                it[TelemetryRecords.deviceId] = UUID.fromString(frame.deviceId)
                it[timestamp] = frame.timestampMs
                it[payloadJson] = json.encodeToString(TelemetryFrame.serializer(), mapped)
            }
        }
    }
}

class RecipeRepositoryImpl : RecipeRepository {
    private val json = Json { encodeDefaults = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    override suspend fun create(recipe: Recipe): Recipe = transaction {
        Recipes.insert {
            it[id] = UUID.fromString(recipe.id.value)
            it[name] = recipe.name
            it[description] = recipe.description
            it[parametersJson] = json.encodeToString(mapSerializer, recipe.parameters)
            it[active] = recipe.active
            it[createdAt] = recipe.createdAt.toEpochMilliseconds()
            it[updatedAt] = recipe.updatedAt.toEpochMilliseconds()
        }
        recipe
    }

    override suspend fun update(recipe: Recipe): Recipe = transaction {
        Recipes.update({ Recipes.id eq UUID.fromString(recipe.id.value) }) {
            it[name] = recipe.name
            it[description] = recipe.description
            it[parametersJson] = json.encodeToString(mapSerializer, recipe.parameters)
            it[active] = recipe.active
            it[updatedAt] = recipe.updatedAt.toEpochMilliseconds()
        }
        recipe
    }

    override suspend fun list(): List<Recipe> = transaction {
        Recipes.selectAll()
            .orderBy(Recipes.updatedAt, SortOrder.DESC)
            .map {
                Recipe(
                    id = RecipeId(it[Recipes.id].value.toString()),
                    name = it[Recipes.name],
                    description = it[Recipes.description],
                    parameters = json.decodeFromString(mapSerializer, it[Recipes.parametersJson]),
                    active = it[Recipes.active],
                    createdAt = Instant.fromEpochMilliseconds(it[Recipes.createdAt]),
                    updatedAt = Instant.fromEpochMilliseconds(it[Recipes.updatedAt])
                )
            }
    }

    override suspend fun setActive(id: RecipeId, active: Boolean) {
        transaction {
            Recipes.update({ Recipes.id eq UUID.fromString(id.value) }) {
                it[Recipes.active] = active
                it[Recipes.updatedAt] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun findById(id: RecipeId): Recipe? = transaction {
        Recipes.select { Recipes.id eq UUID.fromString(id.value) }
            .map {
                Recipe(
                    id = RecipeId(it[Recipes.id].value.toString()),
                    name = it[Recipes.name],
                    description = it[Recipes.description],
                    parameters = json.decodeFromString(mapSerializer, it[Recipes.parametersJson]),
                    active = it[Recipes.active],
                    createdAt = Instant.fromEpochMilliseconds(it[Recipes.createdAt]),
                    updatedAt = Instant.fromEpochMilliseconds(it[Recipes.updatedAt])
                )
            }
            .singleOrNull()
    }
}

class EvidenceRepositoryImpl : EvidenceRepository {
    override suspend fun append(log: AuditLog) {
        transaction {
            AuditLogs.insert {
                it[id] = UUID.fromString(log.id)
                it[jobId] = UUID.fromString(log.jobId.value)
                it[actorId] = UUID.fromString(log.actorId.value)
                it[deviceId] = UUID.fromString(log.deviceId.value)
                it[eventType] = log.eventType
                it[payloadHash] = log.payloadHash
                it[hash] = log.hash
                it[prevHash] = log.prevHash
                it[timestamp] = log.timestamp.toEpochMilliseconds()
            }
        }
    }

    override suspend fun list(jobId: PrintJobId): List<AuditLog> = transaction {
        AuditLogs.select { AuditLogs.jobId eq UUID.fromString(jobId.value) }
            .orderBy(AuditLogs.timestamp, SortOrder.ASC)
            .map {
                AuditLog(
                    id = it[AuditLogs.id].value.toString(),
                    jobId = PrintJobId(it[AuditLogs.jobId].value.toString()),
                    actorId = UserId(it[AuditLogs.actorId].value.toString()),
                    deviceId = DeviceId(it[AuditLogs.deviceId].value.toString()),
                    eventType = it[AuditLogs.eventType],
                    payloadHash = it[AuditLogs.payloadHash],
                    hash = it[AuditLogs.hash],
                    prevHash = it[AuditLogs.prevHash],
                    timestamp = Instant.fromEpochMilliseconds(it[AuditLogs.timestamp])
                )
            }
    }

    override suspend fun lastHash(jobId: PrintJobId): String? = transaction {
        AuditLogs.select { AuditLogs.jobId eq UUID.fromString(jobId.value) }
            .orderBy(AuditLogs.timestamp, SortOrder.DESC)
            .limit(1)
            .map { it[AuditLogs.hash] }
            .singleOrNull()
    }
}

class BioinkRepositoryImpl : BioinkRepository {
    override suspend fun createProfile(profile: BioinkProfile): BioinkProfile {
        transaction {
            BioinkProfiles.insert {
                it[id] = UUID.fromString(profile.id.value)
                it[name] = profile.name
                it[manufacturer] = profile.manufacturer
                it[viscosityModel] = profile.viscosityModel
                it[createdAt] = profile.createdAt.toEpochMilliseconds()
            }
        }
        return profile
    }

    override suspend fun createBatch(batch: BioinkBatch): BioinkBatch {
        transaction {
            BioinkBatches.insert {
                it[id] = UUID.fromString(batch.id.value)
                it[profileId] = UUID.fromString(batch.profileId.value)
                it[lotNumber] = batch.lotNumber
                it[manufacturer] = batch.manufacturer
                it[createdAt] = batch.createdAt.toEpochMilliseconds()
                it[expiresAt] = batch.expiresAt.toEpochMilliseconds()
            }
        }
        return batch
    }

    override suspend fun listProfiles(): List<BioinkProfile> = transaction {
        BioinkProfiles.selectAll().map {
            BioinkProfile(
                id = BioinkProfileId(it[BioinkProfiles.id].value.toString()),
                name = it[BioinkProfiles.name],
                manufacturer = it[BioinkProfiles.manufacturer],
                viscosityModel = it[BioinkProfiles.viscosityModel],
                createdAt = Instant.fromEpochMilliseconds(it[BioinkProfiles.createdAt])
            )
        }
    }

    override suspend fun listBatches(): List<BioinkBatch> = transaction {
        BioinkBatches.selectAll().map {
            BioinkBatch(
                id = BioinkBatchId(it[BioinkBatches.id].value.toString()),
                profileId = BioinkProfileId(it[BioinkBatches.profileId].value.toString()),
                lotNumber = it[BioinkBatches.lotNumber],
                manufacturer = it[BioinkBatches.manufacturer],
                createdAt = Instant.fromEpochMilliseconds(it[BioinkBatches.createdAt]),
                expiresAt = Instant.fromEpochMilliseconds(it[BioinkBatches.expiresAt])
            )
        }
    }
}

