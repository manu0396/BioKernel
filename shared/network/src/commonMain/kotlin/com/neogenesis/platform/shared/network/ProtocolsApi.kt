package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.PublishProtocolVersionRequestDto
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody

interface ProtocolsApi {
    suspend fun list(): ApiResult<List<Protocol>>
    suspend fun get(protocolId: String): ApiResult<Protocol>
    suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion>
}

class KtorProtocolsApi(
    private val client: HttpClient,
    private val logger: AppLogger = NoOpLogger
) : ProtocolsApi {
    override suspend fun list(): ApiResult<List<Protocol>> {
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.get("/api/v1/protocols") {
                header("X-Correlation-Id", correlationId)
            }
        }
    }

    override suspend fun get(protocolId: String): ApiResult<Protocol> {
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.get("/api/v1/protocols/$protocolId") {
                header("X-Correlation-Id", correlationId)
            }
        }
    }

    override suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion> {
        val correlationId = CorrelationIds.newId()
        val request = PublishProtocolVersionRequestDto()
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/protocols/$protocolId/versions/$versionId/publish") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }
}
