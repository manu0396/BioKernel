package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.RunControlRequestDto
import com.neogenesis.platform.data.api.StartRunRequestDto
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.domain.RunStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface RunsApi {
    suspend fun list(): ApiResult<List<Run>>
    suspend fun start(protocolId: String, versionId: String): ApiResult<Run>
    suspend fun updateStatus(runId: String, status: RunStatus): ApiResult<Run>
    suspend fun events(runId: String): ApiResult<List<RunEvent>>
}

class KtorRunsApi(
    private val client: HttpClient,
    private val logger: AppLogger = NoOpLogger
) : RunsApi {
    override suspend fun list(): ApiResult<List<Run>> {
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.get("/api/v1/runs") { header("X-Correlation-Id", correlationId) }
        }
    }

    override suspend fun start(protocolId: String, versionId: String): ApiResult<Run> {
        val correlationId = CorrelationIds.newId()
        val request = StartRunRequestDto(protocolId, versionId)
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/runs/start") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }

    override suspend fun updateStatus(runId: String, status: RunStatus): ApiResult<Run> {
        val correlationId = CorrelationIds.newId()
        val request = RunControlRequestDto(status.name)
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/runs/$runId/control") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }

    override suspend fun events(runId: String): ApiResult<List<RunEvent>> {
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.get("/api/v1/runs/$runId/events") { header("X-Correlation-Id", correlationId) }
        }
    }
}
