package com.neogenesis.platform.shared.network

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BillingUiStateTest {
    @Test
    fun mapsUnknownStateWhenNoSnapshot() {
        val uiState = EntitlementsState(isLoading = true).toUpgradeUiState()

        assertEquals("unknown", uiState.planLabel)
        assertEquals("unknown", uiState.statusLabel)
        assertEquals("n/a", uiState.renewalLabel)
        assertTrue(uiState.isLoading)
        assertFalse(uiState.showUnavailableBanner)
        assertTrue(uiState.features.all { !it.enabled })
    }

    @Test
    fun mapsFeatureEnablementFromSnapshot() {
        val state = EntitlementsState(
            snapshot = EntitlementsSnapshot(
                plan = "pro",
                status = SubscriptionStatus.ACTIVE,
                periodEnd = Instant.parse("2026-03-01T00:00:00Z"),
                entitlements = setOf(FeatureFlag.MULTI_DEVICE),
                fetchedAt = Instant.parse("2026-02-23T10:00:00Z"),
                source = EntitlementsSource.NETWORK
            )
        )

        val uiState = state.toUpgradeUiState()

        assertEquals("pro", uiState.planLabel)
        assertEquals("ACTIVE", uiState.statusLabel)
        assertEquals("2026-03-01T00:00:00Z", uiState.renewalLabel)
        assertTrue(uiState.features.first { it.feature == FeatureFlag.MULTI_DEVICE }.enabled)
        assertFalse(uiState.features.first { it.feature == FeatureFlag.AUDIT_EXPORT }.enabled)
        assertFalse(uiState.showUnavailableBanner)
    }

    @Test
    fun requiresPaywallWhenMissingFeature() {
        val state = EntitlementsState(
            snapshot = EntitlementsSnapshot(
                plan = "starter",
                status = SubscriptionStatus.INACTIVE,
                periodEnd = null,
                entitlements = emptySet(),
                fetchedAt = Instant.parse("2026-02-23T10:00:00Z"),
                source = EntitlementsSource.NETWORK
            )
        )

        assertTrue(state.requiresPaywall(FeatureFlag.ADVANCED_TELEMETRY_EXPORT))
    }

    @Test
    fun unavailableBannerShownWhenErrorPresent() {
        val uiState = EntitlementsState(error = "billing_status_unavailable").toUpgradeUiState()
        assertTrue(uiState.showUnavailableBanner)
    }
}
