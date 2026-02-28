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
        val response: com.neogenesis.platform.proto.v1.ListProtocolsResponse =
            client.get("/api/v1/regenops/protocols").body()
        ApiResult.Success(response.protocolsList.map { it.toDomain() })
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion> {
        return ApiResult.Failure(NetworkError.UnknownError("not_supported"))
    }

    override suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run> = runCatching {
        val response: com.neogenesis.platform.proto.v1.RunRef = client.post("/api/v1/regenops/runs/start") {
            setBody(mapOf("protocolId" to protocolId, "versionId" to versionId))
        }.body()
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun pauseRun(runId: String): ApiResult<Run> =
        ApiResult.Failure(NetworkError.UnknownError("not_supported"))

    override suspend fun abortRun(runId: String): ApiResult<Run> =
        ApiResult.Failure(NetworkError.UnknownError("not_supported"))
}

private fun com.neogenesis.platform.proto.v1.ProtocolSummary.toDomain(): Protocol {
    val latest = latestVersion.toDomain()
    return Protocol(
        id = ProtocolId(protocolId),
        name = name,
        summary = summary,
        latestVersion = latest,
        versions = listOf(latest)
    )
}

private fun com.neogenesis.platform.proto.v1.ProtocolVersion.toDomain(): ProtocolVersion {
    return ProtocolVersion(
        id = ProtocolVersionId(versionId),
        protocolId = ProtocolId(protocolId),
        version = version,
        createdAt = Clock.System.now(),
        author = "system",
        payload = payload,
        published = published
    )
}

private fun com.neogenesis.platform.proto.v1.RunRef.toDomain(): Run {
    val status = runCatching { RunStatus.valueOf(status) }.getOrElse { RunStatus.PENDING }
    return Run(
        id = RunId(runId),
        protocolId = ProtocolId(""),
        protocolVersionId = ProtocolVersionId(""),
        status = status,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
