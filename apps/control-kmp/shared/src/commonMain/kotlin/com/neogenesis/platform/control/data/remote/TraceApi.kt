package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.control.presentation.DriftAlert
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import com.neogenesis.platform.shared.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

interface TraceApi {
    suspend fun getReproducibilityScore(): ApiResult<Int>
    suspend fun listDriftAlerts(): ApiResult<List<DriftAlert>>
}

@Serializable
private data class ReproducibilityScoreResponse(
    val score: Int
)

@Serializable
private data class DriftAlertResponse(
    val id: String,
    val title: String? = null,
    val severity: String = "INFO",
    val message: String,
    val createdAt: String? = null
)

@Serializable
private data class DriftAlertsResponse(
    val alerts: List<DriftAlertResponse> = emptyList()
)

class HttpTraceApi(
    private val client: HttpClient
) : TraceApi {
    override suspend fun getReproducibilityScore(): ApiResult<Int> {
        val result = safeApiCall<ReproducibilityScoreResponse> {
            client.get("/api/v1/metrics/reproducibility-score")
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.value.score)
            is ApiResult.Failure -> result
        }
    }

    override suspend fun listDriftAlerts(): ApiResult<List<DriftAlert>> {
        val result = safeApiCall<DriftAlertsResponse> {
            client.get("/api/v1/metrics/drift-alerts")
        }
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(result.value.alerts.map { it.toDomain() })
            is ApiResult.Failure -> result
        }
    }
}

class DemoTraceApi : TraceApi {
    override suspend fun getReproducibilityScore(): ApiResult<Int> {
        return ApiResult.Success(92)
    }

    override suspend fun listDriftAlerts(): ApiResult<List<DriftAlert>> {
        return ApiResult.Success(
            listOf(
                DriftAlert(
                    id = "demo-1",
                    title = "Spec drift",
                    severity = "WARN",
                    message = "Parameter deviation detected in simulated run."
                )
            )
        )
    }
}

private fun DriftAlertResponse.toDomain(): DriftAlert {
    return DriftAlert(
        id = id,
        title = title ?: "Drift alert",
        severity = severity,
        message = message
    )
}
