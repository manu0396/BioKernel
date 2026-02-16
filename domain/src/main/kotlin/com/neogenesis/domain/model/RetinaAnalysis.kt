package com.neogenesis.domain.model

enum class ToxicityLevel { LOW, MODERATE, HIGH, LETHAL }

data class RetinaAnalysis(
    val id: String,
    val rawHash: String,
    val countryIso: String,
    val timestamp: Long,
    val date: String,
    val toxicity: ToxicityLevel,
    val toxicityScore: Float,
    val compatibilityScore: Double,
    val notes: String
) {
    val isImplantable: Boolean
        get() = toxicity == ToxicityLevel.LOW && compatibilityScore > 90.0
}