package com.neogenesis.platform.shared.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <reified T> safeApiCall(
    logger: AppLogger = NoOpLogger,
    correlationId: String? = null,
    crossinline block: suspend () -> HttpResponse
): ApiResult<T> {
    return try {
        val response = block()
        if (response.status.isSuccess()) {
            ApiResult.Success(response.body())
        } else {
            val message = response.bodyAsText().take(512)
            logger.log(
                level = LogLevel.WARN,
                message = "HTTP call failed",
                metadata = mapOf(
                    "status" to response.status.value.toString(),
                    "bodyPrefix" to Redaction.value(message)
                )
                    .withCorrelationId(correlationId)
            )
            ApiResult.Failure(NetworkError.HttpError(response.status.value, message))
        }
    } catch (ex: SerializationException) {
        logger.log(
            LogLevel.WARN,
            "Serialization error",
            mapOf("reason" to Redaction.value(ex.message ?: "unknown"))
                .withCorrelationId(correlationId)
        )
        ApiResult.Failure(NetworkError.SerializationError(ex.message ?: "serialization_error"))
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: java.net.SocketTimeoutException) {
        logger.log(LogLevel.WARN, "Network timeout", emptyMap<String, String>().withCorrelationId(correlationId))
        ApiResult.Failure(NetworkError.TimeoutError("timeout"))
    } catch (ex: java.io.IOException) {
        logger.log(
            LogLevel.WARN,
            "Connectivity error",
            mapOf("reason" to Redaction.value(ex.message ?: "io_error"))
                .withCorrelationId(correlationId)
        )
        ApiResult.Failure(NetworkError.ConnectivityError(ex.message ?: "io_error"))
    } catch (ex: Exception) {
        logger.log(
            LogLevel.ERROR,
            "Unexpected network error",
            mapOf("reason" to Redaction.value(ex.message ?: "unknown_error"))
                .withCorrelationId(correlationId)
        )
        ApiResult.Failure(NetworkError.UnknownError(ex.message ?: "unknown_error"))
    }
}

@PublishedApi
internal fun Map<String, String>.withCorrelationId(correlationId: String?): Map<String, String> {
    return if (correlationId.isNullOrBlank()) {
        this
    } else {
        this + ("correlation_id" to correlationId)
    }
}
