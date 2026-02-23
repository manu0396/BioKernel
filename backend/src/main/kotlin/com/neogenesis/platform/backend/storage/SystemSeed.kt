package com.neogenesis.platform.backend.storage

import com.neogenesis.platform.backend.security.PasswordHasher
import com.neogenesis.platform.shared.domain.PrintJobStatus
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object SystemIds {
    val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val deviceId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    val bioinkProfileId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    val bioinkBatchId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000004")
    val jobId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000005")
}

object SystemSeed {
    fun ensureSystemEntities() {
        transaction {
            Roles.insertIgnore {
                it[id] = UUID.randomUUID()
                it[name] = "ADMIN"
            }
            if (Users.select { Users.id eq SystemIds.userId }.empty()) {
                Users.insertIgnore {
                    it[id] = SystemIds.userId
                    it[username] = "system"
                    it[passwordHash] = PasswordHasher.hash("system")
                    it[active] = false
                    it[createdAt] = System.currentTimeMillis()
                }
                val adminRoleId = Roles.select { Roles.name eq "ADMIN" }.single()[Roles.id]
                UserRoles.insertIgnore {
                    it[userId] = EntityID(SystemIds.userId, Users)
                    it[roleId] = adminRoleId
                }
            }

            Devices.insertIgnore {
                it[id] = SystemIds.deviceId
                it[serialNumber] = "SYSTEM"
                it[firmwareVersion] = "system"
                it[pairedAt] = null
                it[active] = false
            }

            BioinkProfiles.insertIgnore {
                it[id] = SystemIds.bioinkProfileId
                it[name] = "system-profile"
                it[manufacturer] = "system"
                it[viscosityModel] = "{}"
                it[createdAt] = System.currentTimeMillis()
            }

            BioinkBatches.insertIgnore {
                it[id] = SystemIds.bioinkBatchId
                it[profileId] = SystemIds.bioinkProfileId
                it[lotNumber] = "system-lot"
                it[manufacturer] = "system"
                it[createdAt] = System.currentTimeMillis()
                it[expiresAt] = System.currentTimeMillis()
            }

            PrintJobs.insertIgnore {
                it[id] = SystemIds.jobId
                it[deviceId] = SystemIds.deviceId
                it[operatorId] = SystemIds.userId
                it[bioinkBatchId] = SystemIds.bioinkBatchId
                it[createdAt] = System.currentTimeMillis()
                it[status] = PrintJobStatus.COMPLETED.name
            }
        }
    }
}
