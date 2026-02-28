package com.neogenesis.domain.usecases

import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRetinaAnalysisUseCase(
    private val repository: RetinaRepository
) {
    operator fun invoke(patientId: String): Flow<List<RetinaAnalysis>> {
        return repository.getLocalAnalysis(patientId).map { list ->
            list.sortedByDescending { it.toxicity.ordinal }
        }
    }
}


