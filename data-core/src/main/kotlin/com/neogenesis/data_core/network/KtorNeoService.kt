package com.neogenesis.data_core.network

import android.util.Log
import com.neogenesis.data_core.model.LoginRequest
import com.neogenesis.data_core.model.LoginResponse
import com.neogenesis.data_core.model.RetinaSampleDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class KtorNeoService(
    private val client: HttpClient,
    private val baseUrl: String
) {

    suspend fun login(user: String, pass: String): LoginResponse {
        return try {
            val response = client.post("${baseUrl}/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(user = user, pass = pass))
            }

            if (response.status.isSuccess()) {
                response.body<LoginResponse>()
            } else {
                Log.e("KtorNeoService", "Server error status: ${response.status}")
                LoginResponse(
                    success = false,
                    message = "Error del servidor: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Log.e("KtorNeoService", "Fatal error during auth: ${e.localizedMessage}")
            LoginResponse(
                success = false,
                message = "Error de conexión: No se pudo contactar con el servidor"
            )
        }
    }

    suspend fun fetchRetinaSamples(patientId: String): List<RetinaSampleDto> {
        return try {
            val response = client.get("$baseUrl/retina/samples") {
                parameter("patientId", patientId)
            }

            if (response.status.isSuccess()) {
                response.body<List<RetinaSampleDto>>()
            } else {
                Log.e("KtorNeoService", "Fetch failed with status: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("KtorNeoService", "Fatal error during fetch: ${e.localizedMessage}")
            emptyList()
        }
    }
}