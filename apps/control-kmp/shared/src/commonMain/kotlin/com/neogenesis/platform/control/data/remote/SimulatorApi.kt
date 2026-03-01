package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.control.presentation.SimulationConfig
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

interface SimulatorApi {
    suspend fun startSimulatedRun(protocolId: String, config: SimulationConfig): ApiResult<String>
}

@Serializable
private data class SimulatorRunRequest(
    val protocolId: String,
    val runId: String? = null,
    val samples: Int,
    val intervalMs: Int,
    val failureAt: Int? = null
)

@Serializable
private data class SimulatorRunResponse(
    val runId: String
)

class HttpSimulatorApi(
    private val client: HttpClient
) : SimulatorApi {
    override suspend fun startSimulatedRun(protocolId: String, config: SimulationConfig): ApiResult<String> {
        val samples = ((config.durationMinutes.coerceAtLeast(1) * 60_000) / config.tickMillis.coerceAtLeast(1))
            .coerceIn(10, 600)
        val body = SimulatorRunRequest(
            protocolId = protocolId,
            samples = samples,
            intervalMs = config.tickMillis.coerceAtLeast(1),
            failureAt = null
        )
        val result = safeApiCall<SimulatorRunResponse> {
            client.post("/demo/simulator/runs") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.value.runId)
            is ApiResult.Failure -> result
        }
    }
}
