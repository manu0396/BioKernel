package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.proto.v1.ListProtocolsResponse
import com.neogenesis.platform.proto.v1.ProtocolSummary
import com.neogenesis.platform.proto.v1.ProtocolVersion
import com.neogenesis.platform.proto.v1.RunEvent
import com.neogenesis.platform.proto.v1.RunRef
import com.neogenesis.platform.proto.v1.TelemetryFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import java.time.Instant
import java.util.UUID

internal object RegenOpsInMemoryStore {
    private val protocols = mutableListOf(
        ProtocolSummary.newBuilder()
            .setProtocolId("proto-1")
            .setName("RegenOps Alpha")
            .setSummary("Baseline scaffold protocol")
            .setLatestVersion(
                ProtocolVersion.newBuilder()
                    .setProtocolId("proto-1")
                    .setVersionId("v1")
                    .setVersion("1.0")
                    .setPayload("{\"pressure\":110}")
                    .setPublished(true)
                    .setCreatedAt(Instant.now().toString())
            )
            .build()
    )
    private val runs = mutableMapOf<String, RunRef>()
    private val runEvents = MutableSharedFlow<RunEvent>(extraBufferCapacity = 32)
    private val telemetry = MutableSharedFlow<TelemetryFrame>(extraBufferCapacity = 32)

    fun listProtocols(): ListProtocolsResponse = ListProtocolsResponse.newBuilder()
        .addAllProtocols(protocols)
        .build()

    fun getVersion(protocolId: String, versionId: String): ProtocolVersion? {
        return protocols.firstOrNull { it.protocolId == protocolId }?.latestVersion
            ?.takeIf { it.versionId == versionId }
    }

    fun startRun(protocolId: String, versionId: String): RunRef {
        val runId = "run-${UUID.randomUUID()}"
        val run = RunRef.newBuilder().setRunId(runId).setStatus("RUNNING").build()
        runs[runId] = run
        runEvents.tryEmit(
            RunEvent.newBuilder()
                .setRunId(runId)
                .setEventType("RUN_STARTED")
                .setMessage("Run started for protocol $protocolId/$versionId")
                .setCreatedAt(Instant.now().toString())
                .build()
        )
        return run
    }

    fun updateRun(runId: String, status: String): RunRef {
        val run = RunRef.newBuilder().setRunId(runId).setStatus(status).build()
        runs[runId] = run
        runEvents.tryEmit(
            RunEvent.newBuilder()
                .setRunId(runId)
                .setEventType("RUN_${status}")
                .setMessage("Run status changed to $status")
                .setCreatedAt(Instant.now().toString())
                .build()
        )
        return run
    }

    fun getRun(runId: String): RunRef? = runs[runId]

    fun events(runId: String): Flow<RunEvent> = runEvents.asSharedFlow().filter { it.runId == runId }

    fun telemetry(runId: String): Flow<TelemetryFrame> = telemetry.asSharedFlow().filter { it.runId == runId }

    fun pushTelemetry(runId: String, frames: List<TelemetryFrame>) {
        frames.forEach { telemetry.tryEmit(it.toBuilder().setRunId(runId).build()) }
    }

    fun pushEvents(events: List<RunEvent>) {
        events.forEach { runEvents.tryEmit(it) }
    }
}
