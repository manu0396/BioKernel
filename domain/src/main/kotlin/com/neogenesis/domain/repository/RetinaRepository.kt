package com.neogenesis.domain.repository

import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.RetinaSample
import kotlinx.coroutines.flow.Flow

interface RetinaRepository {
    fun getLocalAnalysis(patientId: String): Flow<List<RetinaAnalysis>>
    suspend fun syncAnalysis(patientId: String)
    suspend fun deleteAnalysisByPatient(patientId: String)
    suspend fun fetchRetinaSamples(patientId: String): Result<List<RetinaSample>>
}







