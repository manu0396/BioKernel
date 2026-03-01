package com.neogenesis.platform.shared.domain.device

fun defaultCapabilitiesFor(tier: DeviceTier): Set<Capability> {
    return when (tier) {
        DeviceTier.TIER_1 -> Capability.values().toSet()
        DeviceTier.TIER_2 -> setOf(
            Capability.LIVE_MONITOR,
            Capability.READ_ONLY_DASHBOARD,
            Capability.ALERTS,
            Capability.QC_REVIEW
        )
        DeviceTier.TIER_3 -> setOf(
            Capability.READ_ONLY_DASHBOARD,
            Capability.ALERTS
        )
    }
}

fun effectiveCapabilities(tier: DeviceTier, deviceClass: DeviceClass, policy: DevicePolicy?): Set<Capability> {
    var effective = defaultCapabilitiesFor(tier)

    if (tier == DeviceTier.TIER_3 && policy?.allowTier3Alerts == false) {
        effective = effective - Capability.ALERTS
    }

    policy?.tierCaps?.get(tier)?.let { caps ->
        effective = effective.intersect(caps)
    }
    policy?.classCaps?.get(deviceClass)?.let { caps ->
        effective = effective.intersect(caps)
    }

    return effective
}
