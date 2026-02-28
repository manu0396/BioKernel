package com.neurogenesis.shared_network.models

import kotlinx.serialization.Serializable

@Serializable
data class RetinaSampleDto(
    val id: String,
    val patientId: String,
    val toxicityScore: Double,
    val timestamp: String
)