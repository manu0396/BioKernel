package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult

interface ControlApi {
    suspend fun listProtocols(): ApiResult<List<Protocol>>
    suspend fun createProtocol(request: CreateProtocolRequest): ApiResult<Protocol>
    suspend fun listRuns(): ApiResult<List<Run>>
    suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion>
    suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run>
    suspend fun pauseRun(runId: String): ApiResult<Run>
    suspend fun abortRun(runId: String): ApiResult<Run>
}
