package com.neogenesis.platform.data.api

import kotlinx.serialization.Serializable

@Serializable
data class StartRunRequestDto(
    val protocolId: String,
    val versionId: String
)

@Serializable
data class RunControlRequestDto(
    val status: String
)

@Serializable
data class PublishProtocolVersionRequestDto(
    val published: Boolean = true
)
