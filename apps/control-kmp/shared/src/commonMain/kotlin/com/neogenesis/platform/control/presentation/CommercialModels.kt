package com.neogenesis.platform.control.presentation

data class CommercialOpportunity(
    val id: String,
    val name: String,
    val stage: String,
    val expectedRevenueEur: Double,
    val probability: Int,
    val notes: String,
    val loiSigned: Boolean
)

data class CommercialPipeline(
    val stages: Map<String, List<CommercialOpportunity>> = emptyMap()
)
