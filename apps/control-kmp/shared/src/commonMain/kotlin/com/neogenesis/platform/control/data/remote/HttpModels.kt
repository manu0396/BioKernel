package com.neogenesis.platform.control.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ListProtocolsResponseDto(
    val protocols: List<ProtocolSummaryDto> = emptyList()
)

@Serializable
data class ProtocolSummaryDto(
    val protocolId: String,
    val title: String,
    val summary: String? = null,
    val status: String? = null,
    val resultSummary: String? = null,
    val lastOutcome: String? = null,
    val resultMetrics: Map<String, String> = emptyMap(),
    val evidenceSummary: String? = null,
    val lastRunTimeline: List<String> = emptyList(),
    val evidenceArtifacts: List<String> = emptyList(),
    val latestVersion: Int
)

@Serializable
data class CreateProtocolRequest(
    val protocolId: String,
    val title: String,
    val summary: String,
    val contentJson: String,
    val author: String,
    val status: String? = null,
    val resultSummary: String? = null,
    val lastOutcome: String? = null,
    val resultMetrics: Map<String, String> = emptyMap(),
    val evidenceSummary: String? = null,
    val lastRunTimeline: List<String> = emptyList(),
    val evidenceArtifacts: List<String> = emptyList()
)

@Serializable
data class ProtocolVersionRecordDto(
    val protocolId: String,
    val version: Int,
    val contentJson: String,
    val publishedBy: String? = null
)

@Serializable
data class RunRecordDto(
    val runId: String,
    val protocolId: String,
    val protocolVersion: Int,
    val status: String
)
