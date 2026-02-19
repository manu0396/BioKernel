package com.neogenesis.data.repository

import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.RetinaSample
import com.neogenesis.domain.model.ToxicityLevel
import com.neogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockRetinaRepository : RetinaRepository {

    private val _localData = MutableStateFlow<List<RetinaAnalysis>>(emptyList())

    override fun getLocalAnalysis(patientId: String): Flow<List<RetinaAnalysis>> {
        return _localData.asStateFlow()
    }

    override suspend fun syncAnalysis(patientId: String) {
        delay(1500)
        val currentTime = System.currentTimeMillis()

        val mockData = listOf(
            RetinaAnalysis(
                id = "BIO-001-${patientId.take(3)}",
                rawHash = "0x${(patientId + "1").hashCode().toString(16).uppercase()}",
                compatibilityScore = 98.2,
                toxicity = ToxicityLevel.LOW,
                toxicityScore = 0.12f,
                timestamp = currentTime - 86400000,
                date = "2026-02-16 09:30",
                notes = "Estructura celular �ptima."
            ),
            RetinaAnalysis(
                id = "BIO-002-${patientId.take(3)}",
                rawHash = "0x${(patientId + "2").hashCode().toString(16).uppercase()}",
                compatibilityScore = 65.4,
                toxicity = ToxicityLevel.MODERATE,
                toxicityScore = 0.45f,
                timestamp = currentTime - 43200000,
                date = "2026-02-16 21:15",
                notes = "Ligera inflamaci�n en tejido perif�rico."
            ),
            RetinaAnalysis(
                id = "BIO-003-${patientId.take(3)}",
                rawHash = "0x${(patientId + "3").hashCode().toString(16).uppercase()}",
                compatibilityScore = 32.1,
                toxicity = ToxicityLevel.HIGH,
                toxicityScore = 0.78f,
                timestamp = currentTime - 3600000,
                date = "2026-02-17 16:00",
                notes = "Presencia de agentes corrosivos detectada."
            ),
            RetinaAnalysis(
                id = "BIO-004-${patientId.take(3)}",
                rawHash = "0xLETHAL_ALPHA",
                compatibilityScore = 8.9,
                toxicity = ToxicityLevel.LETHAL,
                toxicityScore = 1.0f,
                timestamp = currentTime,
                date = "2026-02-17 17:20",
                notes = "ALERTA NIVEL 4: Contaminaci�n biol�gica severa."
            ),
            RetinaAnalysis(
                id = "BIO-005-${patientId.take(3)}",
                rawHash = "0x${(patientId + "5").hashCode().toString(16).uppercase()}",
                compatibilityScore = 95.0,
                toxicity = ToxicityLevel.LOW,
                toxicityScore = 0.15f,
                timestamp = currentTime - 172800000,
                date = "2026-02-15 11:45",
                notes = "Control rutinario sin anomal�as."
            ),
            RetinaAnalysis(
                id = "BIO-MOD-992",
                rawHash = "0x7D2A${patientId.take(2)}",
                compatibilityScore = 62.4,
                toxicity = ToxicityLevel.MODERATE,
                toxicityScore = 0.45f,
                timestamp = System.currentTimeMillis() - 120000,
                date = "2026-02-17 18:25",
                notes = "Inflamaci�n leve detectada en el tejido epitelial. Requiere seguimiento."
            )
        )
        _localData.update { mockData }
    }

    override suspend fun deleteAnalysisByPatient(patientId: String) {
        _localData.update { emptyList() }
    }
    override suspend fun fetchRetinaSamples(patientId: String): Result<List<RetinaSample>> {
        delay(500)

        val mockSamples = listOf(
            RetinaSample(
                id = "S-001",
                toxicityScore = 0.15,
                date = "2026-02-18 10:00"
            ),
            RetinaSample(
                id = "S-002",
                toxicityScore = 0.45,
                date = "2026-02-18 12:00"
            )
        )

        return Result.success(mockSamples)
    }
}


