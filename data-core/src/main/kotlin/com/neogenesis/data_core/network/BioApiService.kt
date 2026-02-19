package com.neogenesis.data_core.network

import android.util.Log
import com.neurogenesis.shared_network.models.RetinaSampleDto
import com.neurogenesis.shared_network.models.LoginRequest
import com.neurogenesis.shared_network.models.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class BioApiService(
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
                Log.e("BioApiService", "Server error status: ${response.status}")
                LoginResponse(
                    success = false,
                    message = "Error del servidor: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Log.e("BioApiService", "Connection error: ${e.localizedMessage}")
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
                Log.e("BioApiService", "Fetch failed with status: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BioApiService", "Fetch error: ${e.localizedMessage}")
            emptyList()
        }
    }
}