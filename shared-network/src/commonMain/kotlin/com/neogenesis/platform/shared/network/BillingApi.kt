package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.BillingSessionRequestDto
import com.neogenesis.platform.data.api.BillingSessionResponseDto
import com.neogenesis.platform.data.api.BillingStatusResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.datetime.Instant

interface BillingApi {
    suspend fun createCheckoutSession(returnUrl: String? = null): ApiResult<String>
    suspend fun createPortalSession(returnUrl: String? = null): ApiResult<String>
    suspend fun getStatus(): ApiResult<BillingStatus>
}

class KtorBillingApi(
    private val client: HttpClient
) : BillingApi {
    override suspend fun createCheckoutSession(returnUrl: String?): ApiResult<String> {
        val result = safeApiCall<BillingSessionResponseDto> {
            client.post("/billing/checkout-session") {
                if (returnUrl != null) {
                    setBody(BillingSessionRequestDto(returnUrl))
                }
            }
        }
        return result.mapValue { it.url }
    }

    override suspend fun createPortalSession(returnUrl: String?): ApiResult<String> {
        val result = safeApiCall<BillingSessionResponseDto> {
            client.post("/billing/portal-session") {
                if (returnUrl != null) {
                    setBody(BillingSessionRequestDto(returnUrl))
                }
            }
        }
        return result.mapValue { it.url }
    }

    override suspend fun getStatus(): ApiResult<BillingStatus> {
        val result = safeApiCall<BillingStatusResponseDto> {
            client.get("/billing/status")
        }
        return result.mapValue { dto ->
            BillingStatus(
                plan = dto.plan,
                status = SubscriptionStatus.fromWireValue(dto.status),
                periodEnd = dto.periodEnd?.let { raw -> runCatching { Instant.parse(raw) }.getOrNull() },
                entitlements = dto.entitlements.mapNotNull(FeatureFlag::fromWireName).toSet()
            )
        }
    }
}

private inline fun <T, R> ApiResult<T>.mapValue(transform: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(value))
        is ApiResult.Failure -> this
    }
}
