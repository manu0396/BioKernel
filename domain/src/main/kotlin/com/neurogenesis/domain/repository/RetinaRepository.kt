package com.neurogenesis.domain.repository

import com.neurogenesis.domain.model.RetinaAnalysis
import kotlinx.coroutines.flow.Flow

interface RetinaRepository {
    fun getLocalAnalysis(): Flow<List<RetinaAnalysis>>
    suspend fun syncAnalysis(patientId: String)
}

