package com.neogenesis.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.neogenesis.data.db.BioKernelDatabase
import com.neogenesis.data_core.network.KtorNeoService
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.ToxicityLevel
import com.neogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RetinaRepositoryImpl(
    private val api: KtorNeoService,
    private val db: BioKernelDatabase
) : RetinaRepository {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun getLocalAnalysis(): Flow<List<RetinaAnalysis>> {
        return db.retinaAnalysisQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    RetinaAnalysis(
                        id = entity.id,
                        rawHash = "0x${entity.id.hashCode().toString(16).uppercase()}",
                        countryIso = "GLO",
                        timestamp = entity.timestamp,
                        date = dateFormatter.format(Date(entity.timestamp)),
                        toxicity = mapToToxicityLevel(entity.toxicity),
                        toxicityScore = mapToxicityToScore(entity.toxicity),
                        compatibilityScore = entity.score,
                        notes = entity.notes ?: ""
                    )
                }
            }
    }

    override suspend fun syncAnalysis(patientId: String) {
        val remoteData = api.fetchRetinaSamples(patientId)

        db.retinaAnalysisQueries.transaction {
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

    private fun mapToToxicityLevel(name: String?): ToxicityLevel {
        return try {
            ToxicityLevel.valueOf(name ?: "LOW")
        } catch (e: Exception) {
            ToxicityLevel.LOW
        }
    }

    private fun mapToxicityToScore(name: String): Float {
        return when (name) {
            "LOW" -> 0.1f
            "MODERATE" -> 0.4f
            "HIGH" -> 0.7f
            "LETHAL" -> 1.0f
            else -> 0.0f
        }
    }
}