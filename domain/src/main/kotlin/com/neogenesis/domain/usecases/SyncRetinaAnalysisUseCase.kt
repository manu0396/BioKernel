package com.neogenesis.domain.usecases

import com.neogenesis.domain.repository.RetinaRepository

class SyncRetinaAnalysisUseCase(
    private val repository: RetinaRepository
) {
    suspend operator fun invoke(patientId: String = "BIO-USER-001") {
        repository.syncAnalysis(patientId)
    }
}



