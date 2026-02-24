package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.control.presentation.CommercialPipeline
import com.neogenesis.platform.control.presentation.CommercialOpportunity
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import kotlinx.datetime.Clock

class DemoControlApi : ControlApi {
    override suspend fun listProtocols(): ApiResult<List<Protocol>> {
        val version = ProtocolVersion(
            id = ProtocolVersionId("v1"),
            protocolId = ProtocolId("proto-1"),
            version = "1.0",
            createdAt = Clock.System.now(),
            author = "demo",
            payload = "{}",
            published = true
        )
        val protocol = Protocol(
            id = ProtocolId("proto-1"),
            name = "Demo Protocol",
            summary = "Demo protocol for smoke test",
            latestVersion = version,
            versions = listOf(version)
        )
        return ApiResult.Success(listOf(protocol))
    }

    override suspend fun publishVersion(protocolId: String, versionId: String) =
        ApiResult.Failure(com.neogenesis.platform.shared.network.NetworkError.UnknownError("not_supported"))

    override suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run> {
        val run = Run(
            id = RunId("run-demo"),
            protocolId = ProtocolId(protocolId),
            protocolVersionId = ProtocolVersionId(versionId),
            status = RunStatus.RUNNING,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now()
        )
        return ApiResult.Success(run)
    }

    override suspend fun pauseRun(runId: String): ApiResult<Run> =
        ApiResult.Failure(com.neogenesis.platform.shared.network.NetworkError.UnknownError("not_supported"))

    override suspend fun abortRun(runId: String): ApiResult<Run> =
        ApiResult.Failure(com.neogenesis.platform.shared.network.NetworkError.UnknownError("not_supported"))
}
