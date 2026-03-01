package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.observability.BusinessMetrics
import com.neogenesis.platform.core.observability.MetricLabels
import com.neogenesis.grpc.GatewayRunEvent
import com.neogenesis.grpc.GatewayTelemetry
import com.neogenesis.grpc.ListProtocolsResponse
import com.neogenesis.grpc.ProtocolDraftRecord
import com.neogenesis.grpc.ProtocolSummary
import com.neogenesis.grpc.ProtocolVersionRecord
import com.neogenesis.grpc.RunEventRecord
import com.neogenesis.grpc.RunRecord
import com.neogenesis.grpc.TelemetryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.UUID

internal object RegenOpsInMemoryStore {
    private val protocols = mutableListOf(
        ProtocolSummary.newBuilder()
            .setProtocolId("proto-1")
            .setTitle("RegenOps Alpha")
            .setLatestVersion(1)
            .setHasDraft(false)
            .setUpdatedAtMs(System.currentTimeMillis())
            .build()
    )
    private val protocolVersions = mutableMapOf<Pair<String, Int>, ProtocolVersionRecord>(
        ("proto-1" to 1) to ProtocolVersionRecord.newBuilder()
            .setProtocolId("proto-1")
            .setVersion(1)
            .setTitle("RegenOps Alpha")
            .setContentJson("""{"pressure":110}""")
            .setCreatedAtMs(System.currentTimeMillis())
            .build()
    )

    private val runs = mutableMapOf<String, RunRecord>()
    private val runEvents = MutableSharedFlow<RunEventRecord>(extraBufferCapacity = 32)
    private val telemetry = MutableSharedFlow<TelemetryRecord>(extraBufferCapacity = 32)
    private var seqCounter = 0L

    fun listProtocols(): ListProtocolsResponse = ListProtocolsResponse.newBuilder()
        .addAllProtocols(protocols)
        .build()

    fun getVersion(protocolId: String, version: Int): ProtocolVersionRecord? =
        protocolVersions[protocolId to version]

    fun createDraft(protocolId: String, title: String, contentJson: String, actorId: String): ProtocolDraftRecord {
        upsertProtocolSummary(protocolId, title, hasDraft = true)
        val now = System.currentTimeMillis()
        return ProtocolDraftRecord.newBuilder()
            .setProtocolId(protocolId)
            .setTitle(title)
            .setContentJson(contentJson)
            .setUpdatedBy(actorId)
            .setCreatedAtMs(now)
            .setUpdatedAtMs(now)
            .build()
    }

    fun updateDraft(protocolId: String, title: String, contentJson: String, actorId: String): ProtocolDraftRecord {
        upsertProtocolSummary(protocolId, title, hasDraft = true)
        val now = System.currentTimeMillis()
        return ProtocolDraftRecord.newBuilder()
            .setProtocolId(protocolId)
            .setTitle(title)
            .setContentJson(contentJson)
            .setUpdatedBy(actorId)
            .setUpdatedAtMs(now)
            .build()
    }

    fun publishVersion(protocolId: String, changelog: String): ProtocolVersionRecord {
        val nextVersion = (protocols.firstOrNull { it.protocolId == protocolId }?.latestVersion ?: 0) + 1
        val record = ProtocolVersionRecord.newBuilder()
            .setProtocolId(protocolId)
            .setVersion(nextVersion)
            .setTitle("RegenOps ${protocolId.uppercase()}")
            .setContentJson("""{"changelog":"$changelog"}""")
            .setCreatedAtMs(System.currentTimeMillis())
            .build()
        protocolVersions[protocolId to nextVersion] = record
        upsertProtocolSummary(protocolId, record.title, hasDraft = false, latestVersion = nextVersion)
        return record
    }

    fun startRun(
        protocolId: String,
        version: Int,
        requestedRunId: String,
        gatewayId: String,
        labels: MetricLabels
    ): RunRecord {
        val runId = if (requestedRunId.isNotBlank()) requestedRunId else "run-${UUID.randomUUID()}"
        val existing = runs[runId]
        val labelsWithProtocol = labels.withProtocol(protocolId, version)
        if (existing != null) {
            BusinessMetrics.runRetried(labelsWithProtocol)
        }
        val now = System.currentTimeMillis()
        val run = RunRecord.newBuilder()
            .setRunId(runId)
            .setProtocolId(protocolId)
            .setProtocolVersion(version)
            .setGatewayId(gatewayId)
            .setStatus("RUNNING")
            .setStartedAtMs(now)
            .setUpdatedAtMs(now)
            .build()
        runs[runId] = run
        BusinessMetrics.runStarted(labelsWithProtocol)
        runEvents.tryEmit(
            RunEventRecord.newBuilder()
                .setRunId(runId)
                .setEventType("RUN_STARTED")
                .setSource("core")
                .setPayloadJson("""{"message":"Run started for protocol $protocolId/$version"}""")
                .setCreatedAtMs(now)
                .setSeq(nextSeq())
                .build()
        )
        return run
    }

    fun updateRun(runId: String, status: String, labels: MetricLabels): RunRecord {
        val now = System.currentTimeMillis()
        val existing = runs[runId]
        val builder = (existing?.toBuilder() ?: RunRecord.newBuilder().setRunId(runId))
            .setStatus(status)
            .setUpdatedAtMs(now)
        if (status == "PAUSED") {
            builder.setPausedAtMs(now)
        }
        if (status == "ABORTED") {
            builder.setAbortedAtMs(now)
            builder.setAbortReason("requested")
        }
        val run = builder.build()
        val labelsWithProtocol = labels.withProtocol(existing?.protocolId, existing?.protocolVersion)
        if (status == "PAUSED") {
            BusinessMetrics.runPaused(labelsWithProtocol)
        }
        if (status == "ABORTED") {
            BusinessMetrics.runFailed(labelsWithProtocol)
            val startedAt = existing?.startedAtMs ?: now
            val durationMs = (now - startedAt).coerceAtLeast(0)
            BusinessMetrics.runDuration(labelsWithProtocol, "failure", durationMs)
        }
        if (status == "COMPLETED") {
            BusinessMetrics.runCompleted(labelsWithProtocol)
            val startedAt = existing?.startedAtMs ?: now
            val durationMs = (now - startedAt).coerceAtLeast(0)
            BusinessMetrics.runDuration(labelsWithProtocol, "success", durationMs)
        }
        if (status == "RUNNING" && existing?.status == "PAUSED") {
            BusinessMetrics.runResumed(labelsWithProtocol)
        }
        runs[runId] = run
        runEvents.tryEmit(
            RunEventRecord.newBuilder()
                .setRunId(runId)
                .setEventType("RUN_$status")
                .setSource("core")
                .setPayloadJson("""{"message":"Run status changed to $status"}""")
                .setCreatedAtMs(now)
                .setSeq(nextSeq())
                .build()
        )
        return run
    }

    fun getRun(runId: String): RunRecord? = runs[runId]

    fun listRuns(): List<RunRecord> = runs.values.sortedByDescending { it.startedAtMs }

    fun runCount(): Int = runs.size

    fun events(runId: String): Flow<RunEventRecord> = runEvents.asSharedFlow().filter { it.runId == runId }

    fun telemetry(runId: String): Flow<TelemetryRecord> = telemetry.asSharedFlow().filter { it.runId == runId }

    fun pushGatewayTelemetry(gatewayId: String, frames: List<GatewayTelemetry>) {
        frames.forEach { frame ->
            BusinessMetrics.telemetryFrames(frame.metricKey)
            telemetry.tryEmit(
                TelemetryRecord.newBuilder()
                    .setRunId(frame.runId)
                    .setGatewayId(gatewayId)
                    .setMetricKey(frame.metricKey)
                    .setMetricValue(frame.metricValue)
                    .setUnit(frame.unit)
                    .setDriftScore(frame.driftScore)
                    .setRecordedAtMs(frame.recordedAtMs)
                    .setSeq(frame.seq)
                    .build()
            )
        }
    }

    fun pushGatewayEvents(events: List<GatewayRunEvent>) {
        events.forEach { event ->
            BusinessMetrics.gatewayEvents(event.eventType, "gateway")
            runEvents.tryEmit(
                RunEventRecord.newBuilder()
                    .setRunId(event.runId)
                    .setEventType(event.eventType)
                    .setSource("gateway")
                    .setPayloadJson(event.payloadJson)
                    .setCreatedAtMs(event.createdAtMs)
                    .setSeq(event.seq)
                    .build()
            )
        }
    }

    fun registerGateway(gatewayId: String, displayName: String): com.neogenesis.grpc.GatewayRecord =
        com.neogenesis.grpc.GatewayRecord.newBuilder()
            .setGatewayId(gatewayId)
            .setDisplayName(displayName.ifBlank { gatewayId })
            .setStatus("REGISTERED")
            .setLastHeartbeatAtMs(System.currentTimeMillis())
            .setUpdatedAtMs(System.currentTimeMillis())
            .build()

    fun heartbeatGateway(gatewayId: String): com.neogenesis.grpc.GatewayRecord =
        com.neogenesis.grpc.GatewayRecord.newBuilder()
            .setGatewayId(gatewayId)
            .setDisplayName(gatewayId)
            .setStatus("OK")
            .setLastHeartbeatAtMs(System.currentTimeMillis())
            .setUpdatedAtMs(System.currentTimeMillis())
            .build()

    private fun upsertProtocolSummary(
        protocolId: String,
        title: String,
        hasDraft: Boolean,
        latestVersion: Int? = null
    ) {
        val index = protocols.indexOfFirst { it.protocolId == protocolId }
        val existing = if (index >= 0) protocols[index] else null
        val summary = ProtocolSummary.newBuilder()
            .setProtocolId(protocolId)
            .setTitle(if (title.isNotBlank()) title else existing?.title ?: protocolId)
            .setLatestVersion(latestVersion ?: existing?.latestVersion ?: 0)
            .setHasDraft(hasDraft)
            .setUpdatedAtMs(System.currentTimeMillis())
            .build()
        if (index >= 0) {
            protocols[index] = summary
        } else {
            protocols.add(summary)
        }
    }

    private fun nextSeq(): Long {
        seqCounter += 1
        return seqCounter
    }
}
