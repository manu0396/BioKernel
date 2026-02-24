package com.neogenesis.platform.core.http

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val correlationId: String? = null
)

