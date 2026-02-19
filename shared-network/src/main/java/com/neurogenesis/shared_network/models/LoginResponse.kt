@file:OptIn(InternalSerializationApi::class)

package com.neurogenesis.shared_network.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
    val patientId: String? = null
)


