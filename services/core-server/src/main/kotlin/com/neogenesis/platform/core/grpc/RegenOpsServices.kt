package com.neogenesis.platform.core.grpc

import com.neogenesis.grpc.CreateDraftRequest
import com.neogenesis.grpc.DiffVersionsRequest
import com.neogenesis.grpc.DiffVersionsResponse
import com.neogenesis.grpc.ExportRunReportRequest
import com.neogenesis.grpc.FetchConfigRequest
import com.neogenesis.grpc.GatewayAck
import com.neogenesis.grpc.GatewayConfig
import com.neogenesis.grpc.GatewayRecord
import com.neogenesis.grpc.GatewayServiceGrpcKt
import com.neogenesis.grpc.GetProtocolVersionRequest
import com.neogenesis.grpc.GetReproducibilityScoreRequest
import com.neogenesis.grpc.GetRunRequest
import com.neogenesis.grpc.HeartbeatRequest
import com.neogenesis.grpc.ListDriftAlertsRequest
import com.neogenesis.grpc.ListDriftAlertsResponse
import com.neogenesis.grpc.ListProtocolsRequest
import com.neogenesis.grpc.ListProtocolsResponse
import com.neogenesis.grpc.MetricsServiceGrpcKt
import com.neogenesis.grpc.ProtocolDraftRecord
import com.neogenesis.grpc.ProtocolServiceGrpcKt
import com.neogenesis.grpc.ProtocolVersionRecord
import com.neogenesis.grpc.PublishVersionRequest
import com.neogenesis.grpc.ReproducibilityScoreResponse
import com.neogenesis.grpc.RegisterGatewayRequest
import com.neogenesis.grpc.RunControlRequest
import com.neogenesis.grpc.RunEventRecord
import com.neogenesis.grpc.RunRecord
import com.neogenesis.grpc.RunReportResponse
import com.neogenesis.grpc.RunServiceGrpcKt
import com.neogenesis.grpc.StartRunRequest
import com.neogenesis.grpc.StreamRunEventsRequest
import com.neogenesis.grpc.StreamTelemetryRequest
import com.neogenesis.grpc.TelemetryRecord
import com.neogenesis.grpc.UpdateDraftRequest
import com.neogenesis.platform.shared.domain.device.Capability
import kotlinx.coroutines.flow.Flow

class RegenOpsProtocolService : ProtocolServiceGrpcKt.ProtocolServiceCoroutineImplBase() {
    override suspend fun createDraft(request: CreateDraftRequest): ProtocolDraftRecord =
        GrpcCapabilityGuard.requireCapability(Capability.PROTOCOL_EDIT).let {
            RegenOpsInMemoryStore.createDraft(
            protocolId = request.protocolId,
            title = request.title,
            contentJson = request.contentJson,
            actorId = request.actorId
        ) }

    override suspend fun updateDraft(request: UpdateDraftRequest): ProtocolDraftRecord =
        GrpcCapabilityGuard.requireCapability(Capability.PROTOCOL_EDIT).let {
            RegenOpsInMemoryStore.updateDraft(
            protocolId = request.protocolId,
            title = request.title,
            contentJson = request.contentJson,
            actorId = request.actorId
        ) }

    override suspend fun listProtocols(request: ListProtocolsRequest): ListProtocolsResponse =
        GrpcCapabilityGuard.requireCapability(Capability.READ_ONLY_DASHBOARD).let {
            RegenOpsInMemoryStore.listProtocols()
        }

    override suspend fun getProtocolVersion(request: GetProtocolVersionRequest): ProtocolVersionRecord =
        GrpcCapabilityGuard.requireCapability(Capability.READ_ONLY_DASHBOARD).let {
            RegenOpsInMemoryStore.getVersion(request.protocolId, request.version)
            ?: ProtocolVersionRecord.newBuilder().setProtocolId(request.protocolId).setVersion(request.version).build()
        }

    override suspend fun diffVersions(request: DiffVersionsRequest): DiffVersionsResponse =
        GrpcCapabilityGuard.requireCapability(Capability.READ_ONLY_DASHBOARD).let {
            DiffVersionsResponse.newBuilder().setSummary("diff not implemented").build()
        }

    override suspend fun publishVersion(request: PublishVersionRequest): ProtocolVersionRecord =
        GrpcCapabilityGuard.requireCapability(Capability.PROTOCOL_EDIT).let {
            RegenOpsInMemoryStore.publishVersion(request.protocolId, request.changelog)
        }
}

class RegenOpsRunService : RunServiceGrpcKt.RunServiceCoroutineImplBase() {
    override suspend fun startRun(request: StartRunRequest): RunRecord =
        GrpcCapabilityGuard.requireCapability(Capability.PRINT_CONTROL).let {
            RegenOpsInMemoryStore.startRun(
            protocolId = request.protocolId,
            version = request.protocolVersion,
            requestedRunId = request.runId,
            gatewayId = request.gatewayId,
            labels = GrpcRequestContext.currentLabels(request.protocolId, request.protocolVersion)
        ) }

    override suspend fun pauseRun(request: RunControlRequest): RunRecord =
        GrpcCapabilityGuard.requireCapability(Capability.PRINT_CONTROL).let {
            RegenOpsInMemoryStore.updateRun(request.runId, "PAUSED", GrpcRequestContext.currentLabels())
        }

    override suspend fun abortRun(request: RunControlRequest): RunRecord =
        GrpcCapabilityGuard.requireCapability(Capability.PRINT_CONTROL).let {
            RegenOpsInMemoryStore.updateRun(request.runId, "ABORTED", GrpcRequestContext.currentLabels())
        }

    override suspend fun getRun(request: GetRunRequest): RunRecord =
        GrpcCapabilityGuard.requireCapability(Capability.QC_REVIEW).let {
            RegenOpsInMemoryStore.getRun(request.runId)
            ?: RunRecord.newBuilder().setRunId(request.runId).setStatus("UNKNOWN").build()
        }

    override fun streamRunEvents(request: StreamRunEventsRequest): Flow<RunEventRecord> =
        GrpcCapabilityGuard.requireCapability(Capability.QC_REVIEW).let {
            RegenOpsInMemoryStore.events(request.runId)
        }

    override fun streamTelemetry(request: StreamTelemetryRequest): Flow<TelemetryRecord> =
        GrpcCapabilityGuard.requireCapability(Capability.LIVE_MONITOR).let {
            RegenOpsInMemoryStore.telemetry(request.runId)
        }
}

class RegenOpsGatewayService : GatewayServiceGrpcKt.GatewayServiceCoroutineImplBase() {
    override suspend fun registerGateway(request: RegisterGatewayRequest): GatewayRecord =
        GrpcCapabilityGuard.requireCapability(Capability.ADMIN_SETTINGS).let {
            RegenOpsInMemoryStore.registerGateway(request.gatewayId, request.displayName)
        }

    override suspend fun heartbeat(request: HeartbeatRequest): GatewayRecord =
        GrpcCapabilityGuard.requireCapability(Capability.LIVE_MONITOR).let {
            RegenOpsInMemoryStore.heartbeatGateway(request.gatewayId)
        }

    override suspend fun pushRunEvents(request: com.neogenesis.grpc.PushRunEventsRequest): GatewayAck {
        GrpcCapabilityGuard.requireCapability(Capability.LIVE_MONITOR)
        RegenOpsInMemoryStore.pushGatewayEvents(request.eventsList)
        return GatewayAck.newBuilder()
            .setAccepted(request.eventsCount)
            .setRejected(0)
            .setMessage("OK")
            .build()
    }

    override suspend fun pushTelemetry(request: com.neogenesis.grpc.PushTelemetryRequest): GatewayAck {
        GrpcCapabilityGuard.requireCapability(Capability.LIVE_MONITOR)
        RegenOpsInMemoryStore.pushGatewayTelemetry(request.gatewayId, request.telemetryList)
        return GatewayAck.newBuilder()
            .setAccepted(request.telemetryCount)
            .setRejected(0)
            .setMessage("OK")
            .build()
    }

    override suspend fun fetchConfig(request: FetchConfigRequest): GatewayConfig =
        GrpcCapabilityGuard.requireCapability(Capability.ADMIN_SETTINGS).let {
            GatewayConfig.newBuilder()
            .setGatewayId(request.gatewayId)
            .setActiveProtocolId("proto-1")
            .setActiveProtocolVersion(1)
            .setProtocolContentJson("{\"pressure\":110}")
            .setIssuedAtMs(System.currentTimeMillis())
            .build()
        }
}

class RegenOpsMetricsService : MetricsServiceGrpcKt.MetricsServiceCoroutineImplBase() {
    override suspend fun getReproducibilityScore(request: GetReproducibilityScoreRequest): ReproducibilityScoreResponse =
        GrpcCapabilityGuard.requireCapability(Capability.QC_REVIEW).let {
            ReproducibilityScoreResponse.newBuilder().setRunId(request.runId).setScore(0.95).build()
        }

    override suspend fun listDriftAlerts(request: ListDriftAlertsRequest): ListDriftAlertsResponse =
        GrpcCapabilityGuard.requireCapability(Capability.QC_REVIEW).let {
            ListDriftAlertsResponse.newBuilder().build()
        }

    override suspend fun exportRunReport(request: ExportRunReportRequest): RunReportResponse =
        GrpcCapabilityGuard.requireCapability(Capability.QC_REVIEW).let {
            RunReportResponse.newBuilder()
            .setRunId(request.runId)
            .setProtocolId("proto-1")
            .setProtocolVersion(1)
            .setStatus("OK")
            .setReproducibilityScore(0.95)
            .setEventsJson("[]")
            .setTelemetryJson("[]")
            .setEvidenceChainValid(true)
            .setGeneratedAtMs(System.currentTimeMillis())
            .build()
        }
}
