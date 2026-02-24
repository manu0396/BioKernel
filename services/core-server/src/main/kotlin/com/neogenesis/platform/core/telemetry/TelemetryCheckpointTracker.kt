package com.neogenesis.platform.core.telemetry

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.UserId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class TelemetryCheckpointTracker(
    private val auditLogger: AuditLogger,
    private val systemActor: UserId,
    private val checkpointEvery: Int = 500
) {
    private val mutex = Mutex()
    private val counters = mutableMapOf<String, Int>()

    suspend fun record(jobId: PrintJobId, deviceId: DeviceId) {
        val count = mutex.withLock {
            val current = (counters[jobId.value] ?: 0) + 1
            counters[jobId.value] = current
            current
        }
        if (count % checkpointEvery == 0) {
            val payload = """{"count":$count,"timestampMs":${Clock.System.now().toEpochMilliseconds()}}"""
            auditLogger.appendEvent(
                jobId = jobId,
                actorId = systemActor,
                deviceId = deviceId,
                eventType = "TELEMETRY_CHECKPOINT",
                payload = payload
            )
        }
    }
}

