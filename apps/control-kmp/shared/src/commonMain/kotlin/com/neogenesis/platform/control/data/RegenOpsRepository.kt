package com.neogenesis.platform.control.data

import com.neogenesis.platform.control.data.local.RegenOpsLocalDataSource
import com.neogenesis.platform.control.data.remote.ControlApi
import com.neogenesis.platform.control.data.stream.RegenOpsStreamClient
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.coroutines.flow.Flow

class RegenOpsRepository(
    private val controlApi: ControlApi,
    private val localData: RegenOpsLocalDataSource,
    private val streamClient: RegenOpsStreamClient
) {
    val protocols: Flow<List<Protocol>> = localData.observeProtocols()
    val runs: Flow<List<Run>> = localData.observeRuns(limit = 20)

    suspend fun refreshProtocols(): ApiResult<List<Protocol>> {
        val result = controlApi.listProtocols()
        if (result is ApiResult.Success) {
            val protocols = result.value
            val versions = protocols.flatMap { protocol ->
                val list = protocol.versions.toMutableList()
                protocol.latestVersion?.let { latest ->
                    if (list.none { it.id == latest.id }) {
                        list.add(0, latest)
                    }
                }
                list
            }
            localData.replaceProtocols(protocols, versions)
        }
        return result
    }

    suspend fun refreshRuns(): ApiResult<List<Run>> {
        val result = controlApi.listRuns()
        if (result is ApiResult.Success) {
            result.value.forEach { run ->
                localData.insertRun(run)
            }
        }
        return result
    }

    suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion> {
        val result = controlApi.publishVersion(protocolId, versionId)
        if (result is ApiResult.Success) {
            localData.insertProtocolVersion(result.value)
        }
        return result
    }

    suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run> {
        val result = controlApi.startRun(protocolId, versionId)
        if (result is ApiResult.Success) {
            localData.insertRun(result.value)
        }
        return result
    }

    suspend fun updateRunStatus(runId: String, status: RunStatus): ApiResult<Run> {
        val result = when (status) {
            RunStatus.PAUSED -> controlApi.pauseRun(runId)
            RunStatus.ABORTED -> controlApi.abortRun(runId)
            else -> ApiResult.Failure(NetworkError.UnknownError("unsupported"))
        }
        if (result is ApiResult.Success) {
            localData.insertRun(result.value)
        }
        return result
    }

    fun streamEvents(runId: String): Flow<RunEvent> = streamClient.streamEvents(runId)

    fun streamTelemetry(runId: String): Flow<TelemetryFrame> = streamClient.streamTelemetry(runId)

    fun close() = streamClient.close()
}
