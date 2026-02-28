package com.neogenesis.domain.model

data class RetinaRecord(
    val id: String,
    val date: String,
    val score: Int,
    val diagnosisDetails: String,
    val imageUrl: String? = null,
    val toxicity: String
)