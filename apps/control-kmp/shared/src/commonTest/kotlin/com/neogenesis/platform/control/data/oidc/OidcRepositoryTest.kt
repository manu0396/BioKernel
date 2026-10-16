package com.neogenesis.platform.control.data.oidc

import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.InMemoryTokenStorage
import com.neogenesis.platform.shared.network.NoOpLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class OidcRepositoryTest {
    @Test
    fun deviceAuthorizationUsesConfiguredIssuerEndpoint() = runTest {
        val client = mockOidcClient { path ->
            when (path) {
                "/.well-known/openid-configuration" -> discoveryResponse()
                "/device" -> response(
                    """{"device_code":"device-123","user_code":"ABCD-EFGH","verification_uri":"https://issuer.test/activate","verification_uri_complete":"https://issuer.test/activate?user_code=ABCD-EFGH","expires_in":300,"interval":2}"""
                )
                else -> error("Unexpected OIDC request: $path")
            }
        }
        val repository = OidcRepository(
            OidcDeviceAuthService(client, NoOpLogger),
            InMemoryTokenStorage(),
            NoOpLogger
        )

        val result = repository.startDeviceAuthorization(OidcConfig("https://issuer.test", "client-1"))

        assertIs<ApiResult.Success<DeviceAuthorization>>(result)
        assertEquals("device-123", result.value.deviceCode)
        assertEquals("ABCD-EFGH", result.value.userCode)
        assertEquals(2, result.value.intervalSeconds)
        client.close()
    }

    @Test
    fun successfulTokenResponseIsStored() = runTest {
        val storage = InMemoryTokenStorage()
        val client = mockOidcClient { path ->
            when (path) {
                "/.well-known/openid-configuration" -> discoveryResponse()
                "/token" -> response(
                    """{"access_token":"test-access-token","refresh_token":"test-refresh-token","token_type":"Bearer","expires_in":3600}"""
                )
                else -> error("Unexpected OIDC request: $path")
            }
        }
        val repository = OidcRepository(
            OidcDeviceAuthService(client, NoOpLogger),
            storage,
            NoOpLogger
        )

        val result = repository.pollForTokens(OidcConfig("https://issuer.test", "client-1"), "device-123", 0)

        assertIs<ApiResult.Success<com.neogenesis.platform.shared.domain.AuthTokens>>(result)
        assertEquals("test-access-token", result.value.accessToken)
        assertEquals("test-access-token", storage.readAccessToken())
        assertEquals("test-refresh-token", storage.readRefreshToken())
        client.close()
    }

    @Test
    fun authorizationPendingIsRetriedUntilTheMockProviderIssuesTokens() = runTest {
        val storage = InMemoryTokenStorage()
        var tokenRequests = 0
        val client = mockOidcClient { path ->
            when (path) {
                "/.well-known/openid-configuration" -> discoveryResponse()
                "/token" -> {
                    tokenRequests += 1
                    if (tokenRequests == 1) {
                        response("""{"error":"authorization_pending"}""", HttpStatusCode.BadRequest)
                    } else {
                        response(
                            """{"access_token":"approved-access-token","refresh_token":"approved-refresh-token","token_type":"Bearer","expires_in":3600}"""
                        )
                    }
                }
                else -> error("Unexpected OIDC request: $path")
            }
        }
        val repository = OidcRepository(
            OidcDeviceAuthService(client, NoOpLogger),
            storage,
            NoOpLogger
        )

        val result = repository.pollForTokens(OidcConfig("https://issuer.test", "client-1"), "device-123", 0)

        assertIs<ApiResult.Success<com.neogenesis.platform.shared.domain.AuthTokens>>(result)
        assertEquals(2, tokenRequests)
        assertEquals("approved-access-token", storage.readAccessToken())
        client.close()
    }

    @Test
    fun mockIssuerCannotFabricateSuccessfulAuthentication() = runTest {
        val storage = InMemoryTokenStorage()
        val client = mockOidcClient { path ->
            when (path) {
                "/.well-known/openid-configuration" -> discoveryResponse()
                "/token" -> response(
                    """{"error":"access_denied","error_description":"The test user denied access"}""",
                    HttpStatusCode.BadRequest
                )
                else -> error("Unexpected OIDC request: $path")
            }
        }
        val repository = OidcRepository(
            OidcDeviceAuthService(client, NoOpLogger),
            storage,
            NoOpLogger
        )

        val result = repository.pollForTokens(OidcConfig("http://mock-auth", "test-client"), "device-123", 0)

        assertIs<ApiResult.Failure>(result)
        assertFalse(repository.hasTokens())
        assertEquals(null, storage.readRefreshToken())
        client.close()
    }

    private fun mockOidcClient(handler: (String) -> Pair<String, HttpStatusCode>): HttpClient =
        HttpClient(MockEngine { request ->
            val (body, status) = handler(request.url.encodedPath)
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }) {
            install(ContentNegotiation) {
                json()
            }
        }

    private fun discoveryResponse() = response(
        """{"device_authorization_endpoint":"https://issuer.test/device","token_endpoint":"https://issuer.test/token"}"""
    )

    private fun response(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ): Pair<String, HttpStatusCode> = body to status
}
