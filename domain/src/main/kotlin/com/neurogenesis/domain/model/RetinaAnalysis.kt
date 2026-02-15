package com.neurogenesis.domain.model

enum class ToxicityLevel { LOW, MODERATE, HIGH, LETHAL }

data class RetinaAnalysis(
    val id: String,
    val timestamp: Long,
    val toxicity: ToxicityLevel,
    val compatibilityScore: Double,
    val notes: String
) {
    // Domain Logic: Is this sample safe for human implant?
    val isImplantable: Boolean
        get() = toxicity == ToxicityLevel.LOW && compatibilityScore > 90.0
}