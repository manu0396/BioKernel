package com.neogenesis.platform.control.device

import com.neogenesis.platform.shared.domain.device.DeviceClass
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DeviceTier
import java.io.File
import java.util.UUID

actual fun detectDeviceInfo(appVersion: String, policyVersion: Int?): DeviceInfo {
    val overrides = DeviceOverride.fromSystemProperties()
    val deviceClass = overrides.deviceClass ?: resolveDeviceClass()
    val tier = overrides.deviceTier ?: defaultTier(deviceClass)
    val osName = System.getProperty("os.name") ?: "unknown"
    val osVersion = System.getProperty("os.version")
    return DeviceInfo(
        deviceId = loadOrCreateDeviceId(),
        deviceClass = deviceClass,
        tier = tier,
        appVersion = appVersion,
        platform = "desktop",
        model = osName,
        osVersion = osVersion,
        policyVersion = policyVersion
    )
}

private fun resolveDeviceClass(): DeviceClass {
    val osName = System.getProperty("os.name")?.lowercase() ?: ""
    return when {
        osName.contains("mac") -> DeviceClass.MAC_DESKTOP
        osName.contains("win") -> DeviceClass.WINDOWS_DESKTOP
        else -> DeviceClass.UNKNOWN
    }
}

private fun defaultTier(deviceClass: DeviceClass): DeviceTier {
    return when (deviceClass) {
        DeviceClass.WINDOWS_DESKTOP,
        DeviceClass.MAC_DESKTOP,
        DeviceClass.EMBEDDED_TOUCHSCREEN -> DeviceTier.TIER_1
        DeviceClass.TV_DISPLAY -> DeviceTier.TIER_3
        else -> DeviceTier.TIER_2
    }
}

private fun loadOrCreateDeviceId(): String {
    val dir = File(System.getProperty("user.home"), ".biokernel")
    dir.mkdirs()
    val file = File(dir, "device-id")
    if (file.exists()) {
        val existing = file.readText().trim()
        if (existing.isNotBlank()) return existing
    }
    val created = UUID.randomUUID().toString()
    file.writeText(created)
    return created
}

private data class DeviceOverride(
    val deviceClass: DeviceClass?,
    val deviceTier: DeviceTier?
) {
    companion object {
        fun fromSystemProperties(): DeviceOverride {
            val classOverride = System.getProperty("biokernel.deviceClass")
                ?.trim()
                ?.uppercase()
                ?.let { runCatching { DeviceClass.valueOf(it) }.getOrNull() }
            val tierOverride = System.getProperty("biokernel.deviceTier")
                ?.trim()
                ?.uppercase()
                ?.let { runCatching { DeviceTier.valueOf(it) }.getOrNull() }
            return DeviceOverride(classOverride, tierOverride)
        }
    }
}
