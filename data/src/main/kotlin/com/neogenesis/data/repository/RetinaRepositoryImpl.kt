package com.neogenesis.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.neogenesis.data.db.BioKernelDatabase
import com.neogenesis.data_core.network.KtorNeoService
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.RetinaSample
import com.neogenesis.domain.model.ToxicityLevel
import com.neogenesis.domain.repository.RetinaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RetinaRepositoryImpl(
    private val api: KtorNeoService,
    private val db: BioKernelDatabase
) : RetinaRepository {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun getLocalAnalysis(patientId: String): Flow<List<RetinaAnalysis>> {
        return db.retinaAnalysisQueries
            .selectAll(patientId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    RetinaAnalysis(
                        id = entity.id,
                        rawHash = "0x${entity.id.hashCode().toString(16).uppercase()}",
                        countryIso = "GLO",
                        compatibilityScore = entity.score,
                        toxicity = ToxicityLevel.valueOf(entity.toxicity),
                        toxicityScore = mapToxicityToScore(entity.toxicity),
                        timestamp = entity.timestamp,
                        date = dateFormatter.format(Date(entity.timestamp)),
                        notes = entity.notes ?: ""
                    )
                }
            }
    }

    override suspend fun syncAnalysis(patientId: String): Unit = withContext(Dispatchers.IO) {
        val remoteData = api.fetchRetinaSamples(patientId)
        db.retinaAnalysisQueries.transaction {
            remoteData.forEach { dto ->
                db.retinaAnalysisQueries.insertAnalysis(
                    id = dto.id,
                    patientId = patientId,
                    score = dto.toxicityScore,
                    toxicity = mapScoreToToxicity(dto.toxicityScore).name,
                    timestamp = parseIsoToLong(dto.timestamp),
                    notes = "Synced via BioKernel Cloud"
                )
            }
        }
    }

    override suspend fun fetchRetinaSamples(patientId: String): Result<List<RetinaSample>> {
        return try {
            val dtos = api.fetchRetinaSamples(patientId)

            val domainSamples = dtos.map { dto ->
                RetinaSample(
                    id = dto.id,
                    toxicityScore = dto.toxicityScore,
                    date = dto.timestamp
                )
            }

            Result.success(domainSamples)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAnalysisByPatient(patientId: String): Unit = withContext(Dispatchers.IO) {
        db.retinaAnalysisQueries.deleteByPatient(patientId)
    }

    private fun parseIsoToLong(isoString: String): Long {
        return try {
            ZonedDateTime.parse(isoString).toInstant().toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun mapScoreToToxicity(score: Double): ToxicityLevel {
        return when {
            score < 0.2 -> ToxicityLevel.LOW
            score < 0.5 -> ToxicityLevel.MODERATE
            score < 0.8 -> ToxicityLevel.HIGH
            else -> ToxicityLevel.LETHAL
        }
    }

    private fun mapToxicityToScore(levelName: String): Float {
        return when (levelName) {
            "LOW" -> 0.1f
            "MODERATE" -> 0.4f
            "HIGH" -> 0.7f
            "LETHAL" -> 1.0f
            else -> 0.0f
        }
    }
}