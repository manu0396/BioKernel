package com.neogenesis.platform.shared.evidence

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AuditTrailRecord(
    val id: String,
    val actorId: String,
    val deviceId: String,
    val jobId: String,
    val eventType: String,
    val payloadHash: String,
    val createdAt: Instant
)

interface AuditTrailSink {
    suspend fun append(record: AuditTrailRecord)
}
