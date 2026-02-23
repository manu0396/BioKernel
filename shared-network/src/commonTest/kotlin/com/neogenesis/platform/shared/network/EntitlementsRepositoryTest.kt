package com.neogenesis.platform.shared.network

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class EntitlementsRepositoryTest {
    @Test
    fun usesCacheWithinTtl() = runBlocking {
        val clock = MutableClock(Instant.parse("2026-02-23T10:00:00Z"))
        val api = FakeBillingApi(
            mutableListOf(
                ApiResult.Success(
                    BillingStatus(
                        plan = "pro",
                        status = SubscriptionStatus.ACTIVE,
                        periodEnd = null,
                        entitlements = setOf(FeatureFlag.MULTI_DEVICE)
                    )
                )
            )
        )
        val repository = EntitlementsRepository(
            billingApi = api,
            clock = clock,
            cacheTtl = 10.minutes,
            offlineGrace = 24.hours
        )

        repository.onAppStart()
        clock.nowInstant = Instant.parse("2026-02-23T10:05:00Z")
        repository.refresh(force = false)

        assertEquals(1, api.calls)
        assertEquals(EntitlementsSource.CACHE, repository.state.value.snapshot?.source)
        assertTrue(repository.hasFeature(FeatureFlag.MULTI_DEVICE))
    }

    @Test
    fun forceRefreshBypassesTtl() = runBlocking {
        val clock = MutableClock(Instant.parse("2026-02-23T10:00:00Z"))
        val api = FakeBillingApi(
            mutableListOf(
                ApiResult.Success(
                    BillingStatus(
                        plan = "starter",
                        status = SubscriptionStatus.INACTIVE,
                        periodEnd = null,
                        entitlements = emptySet()
                    )
                ),
                ApiResult.Success(
                    BillingStatus(
                        plan = "pro",
                        status = SubscriptionStatus.ACTIVE,
                        periodEnd = null,
                        entitlements = setOf(FeatureFlag.ADVANCED_TELEMETRY_EXPORT)
                    )
                )
            )
        )
        val repository = EntitlementsRepository(api, clock = clock, cacheTtl = 10.minutes)

        repository.onAppStart()
        clock.nowInstant = Instant.parse("2026-02-23T10:01:00Z")
        repository.onReturnedFromBrowserFlow()

        assertEquals(2, api.calls)
        assertEquals("pro", repository.state.value.snapshot?.plan)
        assertTrue(repository.hasFeature(FeatureFlag.ADVANCED_TELEMETRY_EXPORT))
    }

    @Test
    fun appliesOfflineGraceOnlyForActiveSnapshot() = runBlocking {
        val clock = MutableClock(Instant.parse("2026-02-23T10:00:00Z"))
        val api = FakeBillingApi(
            mutableListOf(
                ApiResult.Success(
                    BillingStatus(
                        plan = "pro",
                        status = SubscriptionStatus.ACTIVE,
                        periodEnd = null,
                        entitlements = setOf(FeatureFlag.AUDIT_EXPORT)
                    )
                ),
                ApiResult.Failure(NetworkError.TimeoutError("timeout"))
            )
        )
        val repository = EntitlementsRepository(api, clock = clock, cacheTtl = 1.minutes, offlineGrace = 24.hours)

        repository.onAppStart()
        clock.nowInstant = Instant.parse("2026-02-23T10:02:00Z")
        repository.refresh(force = false)

        val snapshot = repository.state.value.snapshot
        assertNotNull(snapshot)
        assertEquals(EntitlementsSource.OFFLINE_GRACE, snapshot.source)
        assertTrue(snapshot.entitlements.contains(FeatureFlag.AUDIT_EXPORT))
    }

    @Test
    fun doesNotApplyOfflineGraceForInactiveStatus() = runBlocking {
        val clock = MutableClock(Instant.parse("2026-02-23T10:00:00Z"))
        val api = FakeBillingApi(
            mutableListOf(
                ApiResult.Success(
                    BillingStatus(
                        plan = "free",
                        status = SubscriptionStatus.INACTIVE,
                        periodEnd = null,
                        entitlements = emptySet()
                    )
                ),
                ApiResult.Failure(NetworkError.ConnectivityError("offline"))
            )
        )
        val repository = EntitlementsRepository(api, clock = clock, cacheTtl = 1.minutes, offlineGrace = 24.hours)

        repository.onAppStart()
        clock.nowInstant = Instant.parse("2026-02-23T10:02:00Z")
        repository.refresh(force = false)

        assertNull(repository.state.value.snapshot)
        assertFalse(repository.hasFeature(FeatureFlag.MULTI_DEVICE))
    }
}

private class FakeBillingApi(
    private val results: MutableList<ApiResult<BillingStatus>>
) : BillingApi {
    var calls: Int = 0
        private set

    override suspend fun createCheckoutSession(returnUrl: String?): ApiResult<String> =
        ApiResult.Failure(NetworkError.UnknownError("not_used"))

    override suspend fun createPortalSession(returnUrl: String?): ApiResult<String> =
        ApiResult.Failure(NetworkError.UnknownError("not_used"))

    override suspend fun getStatus(): ApiResult<BillingStatus> {
        calls += 1
        return if (results.isNotEmpty()) {
            results.removeAt(0)
        } else {
            ApiResult.Failure(NetworkError.UnknownError("no_more_results"))
        }
    }
}

private class MutableClock(
    var nowInstant: Instant
) : Clock {
    override fun now(): Instant = nowInstant
}
