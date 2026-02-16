package com.neogenesis.domain.usecases

import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetRetinaAnalysisUseCase(
    private val repository: RetinaRepository
) {
    operator fun invoke(): Flow<List<RetinaAnalysis>> {
        // Now consuming the Local Flow (Room/Cache)
        return repository.getLocalAnalysis().map { list ->
            list.sortedByDescending { it.toxicity.ordinal }
        }
    }
}



