package com.neurogenesis.domain.usecases

import com.neurogenesis.domain.repository.RetinaRepository

class SyncRetinaAnalysisUseCase(
    private val repository: RetinaRepository
) {
    suspend operator fun invoke(patientId: String) {
        repository.syncAnalysis(patientId)
    }
}