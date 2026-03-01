package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.datetime.Clock

class HttpControlApi(
    private val client: HttpClient
) : ControlApi {
    override suspend fun listProtocols(): ApiResult<List<Protocol>> = runCatching {
        val response: ListProtocolsResponseDto =
            client.get("/api/v1/regenops/protocols").body()
        ApiResult.Success(response.protocols.map { it.toDomain() })
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun createProtocol(request: CreateProtocolRequest): ApiResult<Protocol> = runCatching {
        val response: ProtocolSummaryDto =
            client.post("/api/v1/regenops/protocols") { setBody(request) }.body()
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun listRuns(): ApiResult<List<Run>> = runCatching {
        val response: List<RunRecordDto> = client.get("/api/v1/regenops/runs").body()
        ApiResult.Success(response.map { it.toDomain() })
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion> = runCatching {
        val response: ProtocolVersionRecordDto = client.post("/api/v1/regenops/protocols/${'$'}protocolId/publish") {
            setBody(mapOf("versionId" to versionId))
        }.body()
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run> = runCatching {
        val protocolVersion = versionId.filter(Char::isDigit).toIntOrNull() ?: 1
        val response: RunRecordDto = client.post("/api/v1/regenops/runs/start") {
            setBody(mapOf("protocolId" to protocolId, "protocolVersion" to protocolVersion))
        }.body()
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun pauseRun(runId: String): ApiResult<Run> = runCatching {
        val response: RunRecordDto = client.post("/api/v1/regenops/runs/${'$'}runId/pause").body()
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun abortRun(runId: String): ApiResult<Run> = runCatching {
        val response: RunRecordDto = client.post("/api/v1/regenops/runs/${'$'}runId/abort").body()
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }
}

private fun ProtocolSummaryDto.toDomain(): Protocol {
    val latest = ProtocolVersion(
        id = ProtocolVersionId("${protocolId}-v$latestVersion"),
        protocolId = ProtocolId(protocolId),
        version = latestVersion.toString(),
        createdAt = Clock.System.now(),
        author = "system",
        payload = "",
        published = true
    )
    return Protocol(
        id = ProtocolId(protocolId),
        name = title,
        summary = summary ?: "",
        status = status ?: "DRAFT",
        resultSummary = resultSummary,
        lastOutcome = lastOutcome,
        resultMetrics = resultMetrics,
        evidenceSummary = evidenceSummary,
        lastRunTimeline = lastRunTimeline,
        evidenceArtifacts = evidenceArtifacts,
        latestVersion = latest,
        versions = listOf(latest)
    )
}

private fun ProtocolVersionRecordDto.toDomain(): ProtocolVersion {
    return ProtocolVersion(
        id = ProtocolVersionId("${protocolId}-v$version"),
        protocolId = ProtocolId(protocolId),
        version = version.toString(),
        createdAt = Clock.System.now(),
        author = publishedBy?.ifBlank { "system" } ?: "system",
        payload = contentJson,
        published = true
    )
}

private fun RunRecordDto.toDomain(): Run {
    val status = runCatching { RunStatus.valueOf(status) }.getOrElse { RunStatus.PENDING }
    return Run(
        id = RunId(runId),
        protocolId = ProtocolId(protocolId),
        protocolVersionId = ProtocolVersionId(protocolVersion.toString()),
        status = status,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
