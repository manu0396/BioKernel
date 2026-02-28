package com.neogenesis.platform.data.api

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val username: String, val password: String)

@Serializable
data class RegisterRequestDto(val username: String, val password: String, val roles: Set<String>)

@Serializable
data class TokenResponseDto(val accessToken: String, val refreshToken: String)

@Serializable
data class LogoutRequestDto(val refreshToken: String)
