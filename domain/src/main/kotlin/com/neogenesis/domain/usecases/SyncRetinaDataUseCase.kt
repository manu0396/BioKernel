package com.neogenesis.domain.usecases

import com.neogenesis.domain.model.RetinaSample
import com.neogenesis.domain.repository.RetinaRepository

class SyncRetinaDataUseCase(
    private val repository: RetinaRepository
) {
    suspend operator fun invoke(patientId: String): Result<List<RetinaSample>> {
        return repository.fetchRetinaSamples(patientId)
    }
}


