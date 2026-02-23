package com.neogenesis.platform.shared.network

data class UpgradeFeatureState(
    val feature: FeatureFlag,
    val enabled: Boolean
)

data class UpgradeUiState(
    val planLabel: String,
    val statusLabel: String,
    val renewalLabel: String,
    val features: List<UpgradeFeatureState>,
    val showUnavailableBanner: Boolean,
    val isLoading: Boolean
)

fun EntitlementsState.toUpgradeUiState(): UpgradeUiState {
    val snapshot = snapshot
    return UpgradeUiState(
        planLabel = snapshot?.plan ?: "unknown",
        statusLabel = snapshot?.status?.name ?: "unknown",
        renewalLabel = snapshot?.periodEnd?.toString() ?: "n/a",
        features = FeatureFlag.entries.map { flag ->
            UpgradeFeatureState(
                feature = flag,
                enabled = snapshot?.entitlements?.contains(flag) == true
            )
        },
        showUnavailableBanner = error != null,
        isLoading = isLoading
    )
}

fun EntitlementsState.requiresPaywall(flag: FeatureFlag): Boolean =
    snapshot?.entitlements?.contains(flag) != true
