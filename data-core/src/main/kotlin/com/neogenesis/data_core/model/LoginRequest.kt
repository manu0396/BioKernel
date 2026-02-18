@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.neogenesis.data_core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val user: String,
    val pass: String
)


