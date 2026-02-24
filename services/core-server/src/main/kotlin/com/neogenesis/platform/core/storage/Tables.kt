package com.neogenesis.platform.core.storage

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table

object Roles : UUIDTable("roles") {
    val name = varchar("name", 64).uniqueIndex()
}

object Users : UUIDTable("users") {
    val username = varchar("username", 128).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val active = bool("active")
    val createdAt = long("created_at")
}

object UserRoles : Table("user_roles") {
    val userId = reference("user_id", Users)
    val roleId = reference("role_id", Roles)
    override val primaryKey = PrimaryKey(userId, roleId)
}

object Devices : UUIDTable("devices") {
    val serialNumber = varchar("serial_number", 128).uniqueIndex()
    val firmwareVersion = varchar("firmware_version", 64)
    val pairedAt = long("paired_at").nullable()
    val active = bool("active")
}

object DevicePairings : UUIDTable("device_pairings") {
    val deviceId = reference("device_id", Devices)
    val challenge = varchar("challenge", 512)
    val response = varchar("response", 512).nullable()
    val status = varchar("status", 32)
    val createdAt = long("created_at")
    val completedAt = long("completed_at").nullable()
}

object DeviceHealth : UUIDTable("device_health") {
    val deviceId = reference("device_id", Devices)
    val status = varchar("status", 64)
    val details = text("details").nullable()
    val createdAt = long("created_at")
}

object FirmwareVersions : UUIDTable("firmware_versions") {
    val version = varchar("version", 64).uniqueIndex()
    val signedHash = varchar("signed_hash", 256)
    val createdAt = long("created_at")
}

object BioinkProfiles : UUIDTable("bioink_profiles") {
    val name = varchar("name", 128).uniqueIndex()
    val manufacturer = varchar("manufacturer", 128).nullable()
    val viscosityModel = text("viscosity_model")
    val createdAt = long("created_at")
}

object BioinkBatches : UUIDTable("bioink_batches") {
    val profileId = reference("profile_id", BioinkProfiles)
    val lotNumber = varchar("lot_number", 128)
    val manufacturer = varchar("manufacturer", 128)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
}

object Recipes : UUIDTable("recipes") {
    val name = varchar("name", 128).uniqueIndex()
    val description = text("description").nullable()
    val parametersJson = text("parameters_json")
    val active = bool("active")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

object PrintJobs : UUIDTable("print_jobs") {
    val deviceId = reference("device_id", Devices)
    val operatorId = reference("operator_id", Users)
    val bioinkBatchId = reference("bioink_batch_id", BioinkBatches)
    val createdAt = long("created_at")
    val status = varchar("status", 32)
}

object PrintJobParameters : UUIDTable("print_job_parameters") {
    val jobId = reference("job_id", PrintJobs)
    val parametersJson = text("parameters_json")
}

object PrintJobEvents : UUIDTable("print_job_events") {
    val jobId = reference("job_id", PrintJobs)
    val eventType = varchar("event_type", 64)
    val payloadJson = text("payload_json")
    val createdAt = long("created_at")
}

object TelemetryRecords : UUIDTable("telemetry_records") {
    val jobId = reference("job_id", PrintJobs)
    val deviceId = reference("device_id", Devices)
    val timestamp = long("timestamp")
    val payloadJson = text("payload_json")
}

object DigitalTwinMetrics : UUIDTable("digital_twin_metrics") {
    val jobId = reference("job_id", PrintJobs)
    val timestamp = long("timestamp")
    val payloadJson = text("payload_json")
}

object AuditLogs : UUIDTable("audit_logs") {
    val jobId = reference("job_id", PrintJobs)
    val actorId = reference("actor_id", Users)
    val deviceId = reference("device_id", Devices)
    val eventType = varchar("event_type", 64)
    val payloadHash = varchar("payload_hash", 256)
    val hash = varchar("hash", 256)
    val prevHash = varchar("prev_hash", 256).nullable()
    val timestamp = long("timestamp")
}

object HospitalIntegrations : UUIDTable("hospital_integrations") {
    val name = varchar("name", 128)
    val status = varchar("status", 64)
    val createdAt = long("created_at")
}

object IntegrationEvents : UUIDTable("integration_events") {
    val integrationId = reference("integration_id", HospitalIntegrations)
    val eventType = varchar("event_type", 64)
    val payloadJson = text("payload_json")
    val createdAt = long("created_at")
}

