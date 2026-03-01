package com.neogenesis.platform.control.device

import android.content.Context
import android.os.Build
import com.neogenesis.platform.shared.domain.device.DeviceClass
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DeviceTier
import java.util.UUID

actual fun detectDeviceInfo(appVersion: String, policyVersion: Int?): DeviceInfo {
    val context = AndroidDeviceContext.get()
    val overrides = DeviceOverride.fromSystemProperties()
    val deviceClass = overrides.deviceClass ?: resolveDeviceClass(context)
    val tier = overrides.deviceTier ?: defaultTier(deviceClass)
    return DeviceInfo(
        deviceId = loadOrCreateDeviceId(context),
        deviceClass = deviceClass,
        tier = tier,
        appVersion = appVersion,
        platform = "android",
        model = Build.MODEL,
        osVersion = Build.VERSION.RELEASE,
        policyVersion = policyVersion
    )
}

private fun resolveDeviceClass(context: Context): DeviceClass {
    val smallestDp = context.resources.configuration.smallestScreenWidthDp
    return if (smallestDp >= 600) DeviceClass.ANDROID_TABLET else DeviceClass.ANDROID_PHONE
}

private fun defaultTier(deviceClass: DeviceClass): DeviceTier {
    return when (deviceClass) {
        DeviceClass.ANDROID_TABLET,
        DeviceClass.ANDROID_PHONE -> DeviceTier.TIER_2
        else -> DeviceTier.TIER_2
    }
}

private fun loadOrCreateDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences("biokernel_device", Context.MODE_PRIVATE)
    val existing = prefs.getString("device_id", null)
    if (!existing.isNullOrBlank()) return existing
    val created = UUID.randomUUID().toString()
    prefs.edit().putString("device_id", created).apply()
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
