package com.neogenesis.data_core.model

import kotlinx.serialization.Serializable

@Serializable
data class NeoAuthResponse(
    val isSuccessful: Boolean,
    val token: String? = null
)


