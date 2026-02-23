package com.neogenesis.platform.data.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateRecipeRequestDto(
    val name: String,
    val description: String?,
    val parameters: Map<String, String>
)

@Serializable
data class UpdateRecipeRequestDto(
    val name: String,
    val description: String?,
    val parameters: Map<String, String>,
    val active: Boolean
)

@Serializable
data class ActivateRecipeRequestDto(
    val active: Boolean = true
)
