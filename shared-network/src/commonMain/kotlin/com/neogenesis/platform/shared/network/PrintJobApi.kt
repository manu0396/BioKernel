package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.CreatePrintJobRequestDto
import com.neogenesis.platform.data.api.UpdatePrintJobStatusRequestDto
import com.neogenesis.platform.shared.domain.PrintJob
import com.neogenesis.platform.shared.domain.PrintJobStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
    private val client: HttpClient
) : PrintJobApi {
    override suspend fun list(): ApiResult<List<PrintJob>> =
        safeApiCall { client.get("/api/v1/print-jobs") }

    override suspend fun create(
        deviceId: String,
        operatorId: String,
        bioinkBatchId: String,
        parameters: Map<String, String>
    ): ApiResult<PrintJob> {
        val request = CreatePrintJobRequestDto(deviceId, operatorId, bioinkBatchId, parameters)
        return safeApiCall { client.post("/api/v1/print-jobs") { setBody(request) } }
    }

    override suspend fun updateStatus(id: String, status: PrintJobStatus): ApiResult<Unit> {
        val request = UpdatePrintJobStatusRequestDto(status)
        return safeApiCall { client.put("/api/v1/print-jobs/$id/status") { setBody(request) } }
    }
}
