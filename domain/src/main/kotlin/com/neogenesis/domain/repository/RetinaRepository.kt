package com.neogenesis.domain.repository

import com.neogenesis.domain.model.RetinaAnalysis
import kotlinx.coroutines.flow.Flow

interface RetinaRepository {
    fun getLocalAnalysis(): Flow<List<RetinaAnalysis>>
    suspend fun syncAnalysis(patientId: String)
}




