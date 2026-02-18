package com.neogenesis.data_core.model

import kotlinx.serialization.Serializable

@Serializable
data class RetinaSampleDto(
    val id: String,
    val patientId: String,
    val toxicityScore: Double,
    val timestamp: String
)