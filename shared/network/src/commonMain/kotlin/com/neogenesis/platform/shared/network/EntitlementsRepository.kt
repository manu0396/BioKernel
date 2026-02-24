package com.neogenesis.platform.shared.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class EntitlementsRepository(
    private val billingApi: BillingApi,
    private val clock: Clock = Clock.System,
    private val cacheTtl: Duration = 10.minutes,
    private val offlineGrace: Duration = 24.hours
) {
    private val refreshMutex = Mutex()
    private var cached: CachedEntry? = null

    private val _state = MutableStateFlow(EntitlementsState(isLoading = true))
    val state: StateFlow<EntitlementsState> = _state.asStateFlow()

    suspend fun onAppStart() {
        refresh(force = false)
    }

    suspend fun onReturnedFromBrowserFlow() {
        refresh(force = true)
    }

    suspend fun refresh(force: Boolean = false): EntitlementsState {
        return refreshMutex.withLock {
            val now = clock.now()
            val current = cached
            if (!force && current != null && now < current.fetchedAt.plus(cacheTtl)) {
                val snapshot = current.toSnapshot(source = EntitlementsSource.CACHE)
                val next = EntitlementsState(snapshot = snapshot)
                _state.value = next
                return@withLock next
            }

            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = billingApi.getStatus()) {
                is ApiResult.Success -> {
                    val nextCached = CachedEntry(result.value, now)
                    cached = nextCached
                    val snapshot = nextCached.toSnapshot(source = EntitlementsSource.NETWORK)
                    val next = EntitlementsState(snapshot = snapshot)
                    _state.value = next
                    next
                }
                is ApiResult.Failure -> {
                    val graceSnapshot = current?.takeIf { it.status.status.isServiceActive() }
                        ?.takeIf { now < it.fetchedAt.plus(offlineGrace) }
                        ?.toSnapshot(source = EntitlementsSource.OFFLINE_GRACE)

                    val next = EntitlementsState(
                        isLoading = false,
                        snapshot = graceSnapshot,
                        error = "billing_status_unavailable"
                    )
                    _state.value = next
                    next
                }
            }
        }
    }

    fun hasFeature(flag: FeatureFlag): Boolean = _state.value.hasFeature(flag)

    private data class CachedEntry(
        val status: BillingStatus,
        val fetchedAt: Instant
    ) {
        fun toSnapshot(source: EntitlementsSource): EntitlementsSnapshot =
            EntitlementsSnapshot(
                plan = status.plan,
                status = status.status,
                periodEnd = status.periodEnd,
                entitlements = status.entitlements,
                fetchedAt = fetchedAt,
                source = source
            )
    }
}
