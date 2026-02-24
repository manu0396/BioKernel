package com.neogenesis.platform.core.audit

import com.neogenesis.platform.core.storage.EvidenceRepositoryImpl
import com.neogenesis.platform.shared.domain.AuditLog
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.shared.evidence.Hashing
import com.neogenesis.platform.shared.evidence.computeEvidenceHash
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class AuditLogger(
    private val repository: EvidenceRepositoryImpl
) {
    suspend fun appendEvent(
        jobId: PrintJobId,
        actorId: UserId,
        deviceId: DeviceId,
        eventType: String,
        payload: String,
        timestamp: Instant = Clock.System.now()
    ): AuditLog {
        val prevHash = repository.lastHash(jobId)
        val payloadHash = Hashing.sha256(payload)
        val hash = computeEvidenceHash(
            timestamp = timestamp,
            actorId = actorId.value,
            deviceId = deviceId.value,
            jobId = jobId.value,
            eventType = eventType,
            payloadHash = payloadHash,
            prevHash = prevHash
        )
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            jobId = jobId,
            actorId = actorId,
            deviceId = deviceId,
            eventType = eventType,
            payloadHash = payloadHash,
            hash = hash,
            prevHash = prevHash,
            timestamp = timestamp
        )
        repository.append(log)
        return log
    }
}

