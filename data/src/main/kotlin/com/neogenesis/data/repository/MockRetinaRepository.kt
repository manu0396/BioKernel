// data/src/main/kotlin/com/neogenesis/data/repository/MockRetinaRepository.kt
package com.neogenesis.data.repository

import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.ToxicityLevel
import com.neogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockRetinaRepository : RetinaRepository {

    private val _localData = MutableStateFlow<List<RetinaAnalysis>>(emptyList())

    override fun getLocalAnalysis(): Flow<List<RetinaAnalysis>> = _localData.asStateFlow()

    override suspend fun syncAnalysis(patientId: String) {
        delay(1500)
        val currentTime = System.currentTimeMillis()
        val newSamples = listOf(
            RetinaAnalysis(
                id = "MX-${patientId.take(3)}-01",
                rawHash = "0x8A2F${patientId.hashCode()}", // Hash simulado
                countryIso = "MEX", // Localización requerida por UI
                timestamp = currentTime,
                date = "2024-05-20 10:30", // Fecha formateada
                toxicity = ToxicityLevel.LOW,
                toxicityScore = 0.05f, // Score requerido por UI
                compatibilityScore = 99.1,
                notes = "Optimal growth confirmed."
            ),
            RetinaAnalysis(
                id = "PE-${patientId.take(3)}-09",
                rawHash = "0xBC42${patientId.hashCode()}",
                countryIso = "PER",
                timestamp = currentTime,
                date = "2024-05-20 11:15",
                toxicity = ToxicityLevel.HIGH,
                toxicityScore = 0.88f,
                compatibilityScore = 12.4,
                notes = "CRITICAL: Tissue rejection."
            )
        )
        _localData.update { newSamples }
    }
}