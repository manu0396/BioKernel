package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.TokenResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkSmokeTest {
    @Test
    fun safeApiCallParsesJsonPayload() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"accessToken":"access-123","refreshToken":"refresh-456"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val result = safeApiCall<TokenResponseDto> {
            client.get("https://localhost/api/v1/auth/login")
        }

        val success = assertIs<ApiResult.Success<TokenResponseDto>>(result)
        assertEquals("access-123", success.value.accessToken)
        assertEquals("refresh-456", success.value.refreshToken)
        client.close()
    }
}
