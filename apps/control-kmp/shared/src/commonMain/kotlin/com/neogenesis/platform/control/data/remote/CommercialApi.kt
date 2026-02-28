package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.control.presentation.CommercialPipeline
import com.neogenesis.platform.control.presentation.CommercialOpportunity
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class CommercialPipelineResponse(
    val stages: Map<String, List<CommercialOpportunityResponse>>
)

@Serializable
private data class CommercialOpportunityResponse(
    val id: String,
    val name: String,
    val stage: String,
    val expectedRevenueEur: Double,
    val probability: Int,
    val notes: String,
    val loiSigned: Boolean
)

class HttpCommercialApi(
    private val client: HttpClient
) : CommercialApi {
    override suspend fun fetchPipeline(): ApiResult<CommercialPipeline> = runCatching {
        val response: CommercialPipelineResponse = client.get("/api/v1/commercial/pipeline") {
            header("X-Correlation-Id", "corr-${UUID.randomUUID()}")
        }.body()
        val stages = response.stages.mapValues { (_, opportunities) ->
            opportunities.map { it.toDomain() }
        }
        ApiResult.Success(CommercialPipeline(stages))
    }.getOrElse { ApiResult.Failure(NetworkError.ConnectivityError(it.message ?: "unreachable")) }

    override suspend fun exportCsv(): ApiResult<ByteArray> = runCatching {
        val bytes: ByteArray = client.get("/api/v1/commercial/pipeline/export") {
            header("X-Correlation-Id", "corr-${UUID.randomUUID()}")
            header(HttpHeaders.Accept, "text/csv")
        }.body()
        ApiResult.Success(bytes)
    }.getOrElse { ApiResult.Failure(NetworkError.ConnectivityError(it.message ?: "unreachable")) }
}

private fun CommercialOpportunityResponse.toDomain(): CommercialOpportunity {
    return CommercialOpportunity(
        id = id,
        name = name,
        stage = stage,
        expectedRevenueEur = expectedRevenueEur,
        probability = probability,
        notes = notes,
        loiSigned = loiSigned
    )
}
