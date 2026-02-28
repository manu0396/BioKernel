@file:OptIn(InternalSerializationApi::class)

package com.neurogenesis.shared_network.models

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val user: String,
    val pass: String
)


