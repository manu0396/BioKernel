package com.neogenesis.platform.shared.network

import kotlinx.datetime.Instant

enum class FeatureFlag(val wireName: String) {
    AUDIT_EXPORT("AUDIT_EXPORT"),
    ADVANCED_TELEMETRY_EXPORT("ADVANCED_TELEMETRY_EXPORT"),
    MULTI_DEVICE("MULTI_DEVICE");

    companion object {
        fun fromWireName(raw: String): FeatureFlag? =
            entries.firstOrNull { it.wireName.equals(raw, ignoreCase = true) }
    }
}

enum class SubscriptionStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    INACTIVE,
    UNKNOWN;

    fun isServiceActive(): Boolean = this == ACTIVE || this == TRIALING

    companion object {
        fun fromWireValue(raw: String): SubscriptionStatus {
            return when (raw.trim().uppercase()) {
                "ACTIVE" -> ACTIVE
                "TRIALING" -> TRIALING
                "PAST_DUE" -> PAST_DUE
                "CANCELED" -> CANCELED
                "INACTIVE" -> INACTIVE
                else -> UNKNOWN
            }
        }
    }
}

data class BillingStatus(
    val plan: String,
    val status: SubscriptionStatus,
    val periodEnd: Instant?,
    val entitlements: Set<FeatureFlag>
)

enum class EntitlementsSource {
    NETWORK,
    CACHE,
    OFFLINE_GRACE
}

data class EntitlementsSnapshot(
    val plan: String,
    val status: SubscriptionStatus,
    val periodEnd: Instant?,
    val entitlements: Set<FeatureFlag>,
    val fetchedAt: Instant,
    val source: EntitlementsSource
)

data class EntitlementsState(
    val isLoading: Boolean = false,
    val snapshot: EntitlementsSnapshot? = null,
    val error: String? = null
) {
    fun hasFeature(flag: FeatureFlag): Boolean = snapshot?.entitlements?.contains(flag) == true
}
