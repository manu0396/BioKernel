package com.neurogenesis.data.repository

import com.neurogenesis.domain.model.RetinaAnalysis
import com.neurogenesis.domain.model.ToxicityLevel
import com.neurogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MockRetinaRepository : RetinaRepository {

    private val _localData = MutableStateFlow<List<RetinaAnalysis>>(emptyList())

    override fun getLocalAnalysis(): Flow<List<RetinaAnalysis>> {
        return _localData.asStateFlow()
    }

    override suspend fun syncAnalysis(patientId: String) {
        delay(1500)
        val newSamples = listOf(
            RetinaAnalysis(
                id = "MX-${patientId.take(3)}-01",
                timestamp = System.currentTimeMillis(),
                toxicity = ToxicityLevel.LOW,
                compatibilityScore = 99.1,
                notes = "Optimal growth confirmed."
            ),
            RetinaAnalysis(
                id = "MX-${patientId.take(3)}-09",
                timestamp = System.currentTimeMillis(),
                toxicity = ToxicityLevel.HIGH,
                compatibilityScore = 12.4,
                notes = "CRITICAL: Tissue rejection."
            ),
            RetinaAnalysis(
                id = "MX-${patientId.take(3)}-22",
                timestamp = System.currentTimeMillis(),
                toxicity = ToxicityLevel.MODERATE,
                compatibilityScore = 76.0,
                notes = "Minor inflammation."
            )
        )
        _localData.update { newSamples }
    }
}