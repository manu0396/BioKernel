package com.neogenesis.platform.backend.http

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val correlationId: String? = null
)
