package com.neurogenesis.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.neurogenesis.data.db.BioKernelDatabase
import com.neurogenesis.data_core.network.KtorNeoService
import com.neurogenesis.domain.model.RetinaAnalysis
import com.neurogenesis.domain.model.ToxicityLevel
import com.neurogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class RetinaRepositoryImpl(
    private val api: KtorNeoService,
    private val db: BioKernelDatabase
) : RetinaRepository {

    override fun getLocalAnalysis(): Flow<List<RetinaAnalysis>> {
        return db.retinaAnalysisQueries.selectAll { id, _, score, toxicity, timestamp, notes ->
            RetinaAnalysis(
                id = id,
                timestamp = timestamp,
                toxicity = ToxicityLevel.valueOf(toxicity),
                compatibilityScore = score,
                notes = notes ?: ""
            )
        }.asFlow().mapToList(Dispatchers.IO)
    }

    override suspend fun syncAnalysis(patientId: String) {
        val remoteData = api.fetchRetinaSamples(patientId)

        remoteData.forEach { dto ->
            db.retinaAnalysisQueries.insertAnalysis(
                id = dto.sampleId,
                patientId = patientId,
                score = dto.compScore,
                toxicity = dto.toxLevel.name,
                timestamp = dto.createdAt,
                notes = dto.labNotes
            )
        }
    }
}