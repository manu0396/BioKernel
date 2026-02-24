package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.CreatePrintJobRequestDto
import com.neogenesis.platform.data.api.UpdatePrintJobStatusRequestDto
import com.neogenesis.platform.shared.domain.PrintJob
import com.neogenesis.platform.shared.domain.PrintJobStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

interface PrintJobApi {
    suspend fun list(): ApiResult<List<PrintJob>>
    suspend fun create(
        deviceId: String,
        operatorId: String,
        bioinkBatchId: String,
        parameters: Map<String, String>
    ): ApiResult<PrintJob>
    suspend fun updateStatus(id: String, status: PrintJobStatus): ApiResult<Unit>
}

class KtorPrintJobApi(
    private val client: HttpClient,
    private val logger: AppLogger = NoOpLogger
) : PrintJobApi {
    override suspend fun list(): ApiResult<List<PrintJob>> =
        run {
            val correlationId = CorrelationIds.newId()
            safeApiCall(logger, correlationId) {
                client.get("/api/v1/print-jobs") { header("X-Correlation-Id", correlationId) }
            }
        }

    override suspend fun create(
        deviceId: String,
        operatorId: String,
        bioinkBatchId: String,
        parameters: Map<String, String>
    ): ApiResult<PrintJob> {
        val request = CreatePrintJobRequestDto(deviceId, operatorId, bioinkBatchId, parameters)
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/print-jobs") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }

    override suspend fun updateStatus(id: String, status: PrintJobStatus): ApiResult<Unit> {
        val request = UpdatePrintJobStatusRequestDto(status)
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.put("/api/v1/print-jobs/$id/status") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }
}
