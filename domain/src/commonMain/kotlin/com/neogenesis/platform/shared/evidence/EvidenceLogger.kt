package com.neogenesis.platform.shared.evidence

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EvidenceEvent(
    val id: String,
    val timestamp: Instant,
    val actorId: String,
    val deviceId: String,
    val jobId: String,
    val eventType: String,
    val payloadHash: String,
    val hash: String,
    val prevHash: String?
)

@Serializable
data class EvidencePackageExport(
    val eventCount: Int,
    val lastHash: String,
    val json: String
)

class EvidenceChainBuilder {
    private val items = mutableListOf<EvidenceEvent>()

    fun append(event: EvidenceEvent) {
        items.add(event)
    }

    fun createEvent(
        timestamp: Instant,
        actorId: String,
        deviceId: String,
        jobId: String,
        eventType: String,
        payload: String,
        prevHashOverride: String? = null
    ): EvidenceEvent {
        val prevHash = prevHashOverride ?: items.lastOrNull()?.hash
        val payloadHash = Hashing.sha256(payload)
        val hash = computeEvidenceHash(
            timestamp = timestamp,
            actorId = actorId,
            deviceId = deviceId,
            jobId = jobId,
            eventType = eventType,
            payloadHash = payloadHash,
            prevHash = prevHash
        )
        return EvidenceEvent(
            id = hash.take(16),
            timestamp = timestamp,
            actorId = actorId,
            deviceId = deviceId,
            jobId = jobId,
            eventType = eventType,
            payloadHash = payloadHash,
            hash = hash,
            prevHash = prevHash
        )
    }

    fun events(): List<EvidenceEvent> = items.toList()
}

object EvidenceChainValidator {
    fun validate(events: List<EvidenceEvent>): Boolean {
        var prev: String? = null
        for (event in events) {
            if (event.prevHash != prev) return false
            val expected = computeEvidenceHash(
                timestamp = event.timestamp,
                actorId = event.actorId,
                deviceId = event.deviceId,
                jobId = event.jobId,
                eventType = event.eventType,
                payloadHash = event.payloadHash,
                prevHash = event.prevHash
            )
            if (expected != event.hash) return false
            prev = event.hash
        }
        return true
    }
}

fun computeEvidenceHash(
    timestamp: Instant,
    actorId: String,
    deviceId: String,
    jobId: String,
    eventType: String,
    payloadHash: String,
    prevHash: String?
): String {
    val raw = "$timestamp|$actorId|$deviceId|$jobId|$eventType|$payloadHash|${prevHash ?: ""}"
    return Hashing.sha256(raw)
}

object EvidenceExporter {
    private val json = Json { encodeDefaults = true }

    fun export(events: List<EvidenceEvent>): EvidencePackageExport {
        val content = json.encodeToString(events)
        val hash = Hashing.sha256(content)
        return EvidencePackageExport(events.size, hash, content)
    }
}

expect object Hashing {
    fun sha256(input: String): String
}
