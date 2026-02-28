package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.proto.v1.AbortRunRequest
import com.neogenesis.platform.proto.v1.DiffVersionsRequest
import com.neogenesis.platform.proto.v1.DiffVersionsResponse
import com.neogenesis.platform.proto.v1.FetchConfigRequest
import com.neogenesis.platform.proto.v1.FetchConfigResponse
import com.neogenesis.platform.proto.v1.GatewayServiceGrpcKt
import com.neogenesis.platform.proto.v1.GatewayStatus
import com.neogenesis.platform.proto.v1.GetProtocolVersionRequest
import com.neogenesis.platform.proto.v1.GetReproducibilityScoreRequest
import com.neogenesis.platform.proto.v1.GetReproducibilityScoreResponse
import com.neogenesis.platform.proto.v1.GetRunRequest
import com.neogenesis.platform.proto.v1.HeartbeatRequest
import com.neogenesis.platform.proto.v1.ListDriftAlertsRequest
import com.neogenesis.platform.proto.v1.ListDriftAlertsResponse
import com.neogenesis.platform.proto.v1.ListProtocolsRequest
import com.neogenesis.platform.proto.v1.ListProtocolsResponse
import com.neogenesis.platform.proto.v1.MetricsServiceGrpcKt
import com.neogenesis.platform.proto.v1.PauseRunRequest
import com.neogenesis.platform.proto.v1.ProtocolServiceGrpcKt
import com.neogenesis.platform.proto.v1.ProtocolVersion
import com.neogenesis.platform.proto.v1.PublishVersionRequest
import com.neogenesis.platform.proto.v1.RegisterGatewayRequest
import com.neogenesis.platform.proto.v1.RunEvent
import com.neogenesis.platform.proto.v1.RunRef
import com.neogenesis.platform.proto.v1.RunServiceGrpcKt
import com.neogenesis.platform.proto.v1.StartRunRequest
import com.neogenesis.platform.proto.v1.TelemetryFrame
import kotlinx.coroutines.flow.Flow

class RegenOpsProtocolService : ProtocolServiceGrpcKt.ProtocolServiceCoroutineImplBase() {
    override suspend fun listProtocols(request: ListProtocolsRequest): ListProtocolsResponse =
        RegenOpsInMemoryStore.listProtocols()

    override suspend fun getProtocolVersion(request: GetProtocolVersionRequest): ProtocolVersion =
        RegenOpsInMemoryStore.getVersion(request.protocolId, request.versionId)
            ?: ProtocolVersion.newBuilder().setProtocolId(request.protocolId).setVersionId(request.versionId).build()

    override suspend fun diffVersions(request: DiffVersionsRequest): DiffVersionsResponse =
        DiffVersionsResponse.newBuilder().setDiff("diff not implemented").build()

    override suspend fun publishVersion(request: PublishVersionRequest): ProtocolVersion =
        RegenOpsInMemoryStore.getVersion(request.protocolId, request.versionId)
            ?: ProtocolVersion.newBuilder().setProtocolId(request.protocolId).setVersionId(request.versionId).build()
}

class RegenOpsRunService : RunServiceGrpcKt.RunServiceCoroutineImplBase() {
    override suspend fun startRun(request: StartRunRequest): RunRef =
        RegenOpsInMemoryStore.startRun(request.protocolId, request.versionId)

    override suspend fun pauseRun(request: PauseRunRequest): RunRef =
        RegenOpsInMemoryStore.updateRun(request.runId, "PAUSED")

    override suspend fun abortRun(request: AbortRunRequest): RunRef =
        RegenOpsInMemoryStore.updateRun(request.runId, "ABORTED")

    override suspend fun getRun(request: GetRunRequest): RunRef =
        RegenOpsInMemoryStore.getRun(request.runId) ?: RunRef.newBuilder().setRunId(request.runId).setStatus("UNKNOWN").build()

    override fun streamRunEvents(request: GetRunRequest): Flow<RunEvent> =
        RegenOpsInMemoryStore.events(request.runId)

    override fun streamTelemetry(request: GetRunRequest): Flow<TelemetryFrame> =
        RegenOpsInMemoryStore.telemetry(request.runId)
}

class RegenOpsGatewayService : GatewayServiceGrpcKt.GatewayServiceCoroutineImplBase() {
    override suspend fun registerGateway(request: RegisterGatewayRequest): GatewayStatus =
        GatewayStatus.newBuilder().setStatus("REGISTERED").build()

    override suspend fun heartbeat(request: HeartbeatRequest): GatewayStatus =
        GatewayStatus.newBuilder().setStatus("OK").build()

    override suspend fun pushRunEvents(request: com.neogenesis.platform.proto.v1.PushRunEventsRequest): GatewayStatus {
        RegenOpsInMemoryStore.pushEvents(request.eventsList)
        return GatewayStatus.newBuilder().setStatus("OK").build()
    }

    override suspend fun pushTelemetry(request: com.neogenesis.platform.proto.v1.PushTelemetryRequest): GatewayStatus {
        RegenOpsInMemoryStore.pushTelemetry(request.runId, request.framesList)
        return GatewayStatus.newBuilder().setStatus("OK").build()
    }

    override suspend fun fetchConfig(request: FetchConfigRequest): FetchConfigResponse =
        FetchConfigResponse.newBuilder().setConfigJson("{}")
            .build()
}

class RegenOpsMetricsService : MetricsServiceGrpcKt.MetricsServiceCoroutineImplBase() {
    override suspend fun getReproducibilityScore(request: GetReproducibilityScoreRequest): GetReproducibilityScoreResponse =
        GetReproducibilityScoreResponse.newBuilder().setScore(0.95).build()

    override suspend fun listDriftAlerts(request: ListDriftAlertsRequest): ListDriftAlertsResponse =
        ListDriftAlertsResponse.newBuilder().build()

    override suspend fun exportRunReport(request: com.neogenesis.platform.proto.v1.ExportRunReportRequest): com.neogenesis.platform.proto.v1.ExportRunReportResponse =
        com.neogenesis.platform.proto.v1.ExportRunReportResponse.newBuilder().setReportUrl("/reports/${request.runId}").build()
}
