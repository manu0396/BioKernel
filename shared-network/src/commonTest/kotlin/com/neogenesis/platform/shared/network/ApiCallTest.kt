package com.neogenesis.platform.shared.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertTrue

class ApiCallTest {
    @Test
    fun mapsSerializationErrors() = runBlocking {
        val result = safeApiCall<Unit> { throw SerializationException("bad") }
        assertTrue(result is ApiResult.Failure && result.error is NetworkError.SerializationError)
    }

    @Test
    fun mapsTimeoutErrors() = runBlocking {
        val result = safeApiCall<Unit> { throw java.net.SocketTimeoutException("timeout") }
        assertTrue(result is ApiResult.Failure && result.error is NetworkError.TimeoutError)
    }

    @Test
    fun mapsConnectivityErrors() = runBlocking {
        val result = safeApiCall<Unit> { throw java.io.IOException("io") }
        assertTrue(result is ApiResult.Failure && result.error is NetworkError.ConnectivityError)
    }
}
