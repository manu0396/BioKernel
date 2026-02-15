package com.neurogenesis.data_core.network

import com.neurogenesis.domain.model.ToxicityLevel
import kotlinx.coroutines.delay

class KtorNeoService {

    // Mocking the network call for Retina Analysis samples
    suspend fun fetchRetinaSamples(patientId: String): List<RetinaSampleDto> {
        delay(1000)
        return listOf(
            RetinaSampleDto(
                "S-001",
                92.5,
                ToxicityLevel.LOW,
                System.currentTimeMillis(),
                "Clear tissue"
            ),
            RetinaSampleDto(
                "S-002",
                45.0,
                ToxicityLevel.HIGH,
                System.currentTimeMillis(),
                "Rejection detected"
            )
        )
    }

    suspend fun authenticate(user: String, pass: String): NeoAuthResponse {
        delay(500)
        return if (user == "admin") NeoAuthResponse(true) else NeoAuthResponse(false)
    }
}

data class RetinaSampleDto(
    val sampleId: String,
    val compScore: Double,
    val toxLevel: ToxicityLevel,
    val createdAt: Long,
    val labNotes: String?
)

data class NeoAuthResponse(val isSuccessful: Boolean)