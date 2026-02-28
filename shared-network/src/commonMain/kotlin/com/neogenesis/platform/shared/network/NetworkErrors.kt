package com.neogenesis.platform.shared.network

sealed class NetworkError {
    data class HttpError(val status: Int, val message: String) : NetworkError()
    data class SerializationError(val message: String) : NetworkError()
    data class ConnectivityError(val message: String) : NetworkError()
    data class TimeoutError(val message: String) : NetworkError()
    data class UnknownError(val message: String) : NetworkError()
}

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val error: NetworkError) : ApiResult<Nothing>()
}
