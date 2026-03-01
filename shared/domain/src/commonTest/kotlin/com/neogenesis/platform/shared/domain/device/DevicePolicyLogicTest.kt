package com.neogenesis.platform.shared.domain.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevicePolicyLogicTest {
    @Test
    fun defaultCapabilitiesMatchTierPolicy() {
        assertTrue(Capability.PRINT_CONTROL in defaultCapabilitiesFor(DeviceTier.TIER_1))
        assertTrue(Capability.ADMIN_SETTINGS in defaultCapabilitiesFor(DeviceTier.TIER_1))

        val tier2 = defaultCapabilitiesFor(DeviceTier.TIER_2)
        assertEquals(
            setOf(
                Capability.LIVE_MONITOR,
                Capability.READ_ONLY_DASHBOARD,
                Capability.ALERTS,
                Capability.QC_REVIEW
            ),
            tier2
        )

        val tier3 = defaultCapabilitiesFor(DeviceTier.TIER_3)
        assertEquals(
            setOf(
                Capability.READ_ONLY_DASHBOARD,
                Capability.ALERTS
            ),
            tier3
        )
    }

    @Test
    fun effectiveCapabilitiesNarrowWithPolicy() {
        val policy = DevicePolicy(
            version = 1,
            tierCaps = mapOf(
                DeviceTier.TIER_2 to setOf(Capability.READ_ONLY_DASHBOARD, Capability.ALERTS)
            ),
            classCaps = mapOf(
                DeviceClass.ANDROID_TABLET to setOf(Capability.ALERTS)
            )
        )

        val effective = effectiveCapabilities(DeviceTier.TIER_2, DeviceClass.ANDROID_TABLET, policy)
        assertEquals(setOf(Capability.ALERTS), effective)
    }

    @Test
    fun effectiveCapabilitiesKeepDefaultsWhenPolicyMissing() {
        val effective = effectiveCapabilities(DeviceTier.TIER_2, DeviceClass.UNKNOWN, null)
        assertEquals(defaultCapabilitiesFor(DeviceTier.TIER_2), effective)
    }

    @Test
    fun tier3AlertsCanBeDisabledByPolicy() {
        val policy = DevicePolicy(version = 1, allowTier3Alerts = false)
        val effective = effectiveCapabilities(DeviceTier.TIER_3, DeviceClass.TV_DISPLAY, policy)
        assertEquals(setOf(Capability.READ_ONLY_DASHBOARD), effective)
    }
}
