package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

interface ExportsApi {
    suspend fun exportRunReport(runId: String): ApiResult<ExportPayload>
    suspend fun exportAuditBundle(runId: String): ApiResult<ExportPayload>
}

data class ExportPayload(
    val bytes: ByteArray,
    val fileName: String?,
    val contentType: String?
)

class HttpExportsApi(
    private val client: HttpClient
) : ExportsApi {
    override suspend fun exportRunReport(runId: String): ApiResult<ExportPayload> {
        return exportWithMetadata("/api/v1/telemetry/$runId/export", "application/json")
    }

    override suspend fun exportAuditBundle(runId: String): ApiResult<ExportPayload> {
        return exportWithMetadata("/api/v1/evidence/$runId/package", "application/zip")
    }

    private suspend fun exportWithMetadata(path: String, accept: String): ApiResult<ExportPayload> {
        return try {
            val response = client.get(path) {
                header(HttpHeaders.Accept, accept)
            }
            if (response.status.isSuccess()) {
                val fileName = parseFilename(response)
                val contentType = response.headers[HttpHeaders.ContentType]
                ApiResult.Success(ExportPayload(response.body(), fileName, contentType))
            } else {
                val message = response.bodyAsText().take(512)
                ApiResult.Failure(NetworkError.HttpError(response.status.value, message))
            }
        } catch (ex: SerializationException) {
            ApiResult.Failure(NetworkError.SerializationError(ex.message ?: "serialization_error"))
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: java.net.SocketTimeoutException) {
            ApiResult.Failure(NetworkError.TimeoutError("timeout"))
        } catch (ex: java.io.IOException) {
            ApiResult.Failure(NetworkError.ConnectivityError(ex.message ?: "io_error"))
        } catch (ex: Exception) {
            ApiResult.Failure(NetworkError.UnknownError(ex.message ?: "unknown_error"))
        }
    }

    private fun parseFilename(response: HttpResponse): String? {
        val raw = response.headers[HttpHeaders.ContentDisposition] ?: return null
        val match = Regex("filename\\*?=([^;]+)").find(raw) ?: return null
        val value = match.groupValues[1].trim().trim('"')
        return value.substringAfter("''", value)
            .replace("\\", "_")
            .replace("/", "_")
            .replace("..", "_")
            .takeIf { it.isNotBlank() }
    }
}

class DemoExportsApi : ExportsApi {
    override suspend fun exportRunReport(runId: String): ApiResult<ExportPayload> {
        val payload = "{" + "\"runId\":\"$runId\",\"status\":\"demo\"}"
        return ApiResult.Success(ExportPayload(payload.encodeToByteArray(), "run_report_demo.json", "application/json"))
    }

    override suspend fun exportAuditBundle(runId: String): ApiResult<ExportPayload> {
        return ApiResult.Success(ExportPayload(ByteArray(0), "audit_bundle_demo.zip", "application/zip"))
    }
}
