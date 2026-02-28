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
    val latestVersion: Int
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
