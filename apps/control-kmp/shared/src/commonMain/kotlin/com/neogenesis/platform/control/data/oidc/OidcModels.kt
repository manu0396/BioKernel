package com.neogenesis.platform.control.data.oidc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OidcDiscoveryResponse(
    @SerialName("device_authorization_endpoint") val deviceAuthorizationEndpoint: String,
    @SerialName("token_endpoint") val tokenEndpoint: String
)

@Serializable
internal data class OidcDeviceAuthorizationResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("interval") val interval: Int = 5
)

@Serializable
internal data class OidcTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
internal data class OidcErrorResponse(
    val error: String,
    @SerialName("error_description") val errorDescription: String? = null
)

data class OidcConfig(
    val issuer: String,
    val clientId: String,
    val audience: String? = null
)

data class DeviceAuthorization(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val expiresIn: Int,
    val intervalSeconds: Int
)
