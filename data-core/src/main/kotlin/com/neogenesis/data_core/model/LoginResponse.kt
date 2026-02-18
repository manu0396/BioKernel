@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.neogenesis.data_core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
    val patientId: String? = null
)


