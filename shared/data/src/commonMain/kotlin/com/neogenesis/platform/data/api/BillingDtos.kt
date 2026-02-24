package com.neogenesis.platform.data.api

import kotlinx.serialization.Serializable

@Serializable
data class BillingSessionRequestDto(
    val returnUrl: String? = null
)

@Serializable
data class BillingSessionResponseDto(
    val url: String
)

@Serializable
data class BillingStatusResponseDto(
    val plan: String,
    val status: String,
    val periodEnd: String? = null,
    val entitlements: List<String> = emptyList()
)
