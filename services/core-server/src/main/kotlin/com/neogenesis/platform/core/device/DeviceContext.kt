package com.neogenesis.platform.core.device

import com.neogenesis.platform.shared.domain.device.DeviceClass
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DevicePolicy
import com.neogenesis.platform.shared.domain.device.DeviceTier
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.shared.domain.device.effectiveCapabilities
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.util.AttributeKey

data class DeviceRequestContext(
    val deviceInfo: DeviceInfo,
    val effectiveCapabilities: Set<Capability>
)

object DeviceContext {
    private val key = AttributeKey<DeviceRequestContext>("device-context")

    fun fromCall(call: ApplicationCall, policy: DevicePolicy): DeviceRequestContext {
        val existing = call.attributes.getOrNull(key)
        if (existing != null) return existing

        val info = parseDeviceInfo(call)
        val caps = effectiveCapabilities(info.tier, info.deviceClass, policy)
        val ctx = DeviceRequestContext(info, caps)
        call.attributes.put(key, ctx)
        return ctx
    }

    private fun parseDeviceInfo(call: ApplicationCall): DeviceInfo {
        val headers = call.request.headers
        val classHeader = headers["X-Device-Class"]
        val tierHeader = headers["X-Device-Tier"]
        val parsedClass = classHeader?.let { runCatching { DeviceClass.valueOf(it.trim().uppercase()) }.getOrNull() }
        val parsedTier = tierHeader?.let { runCatching { DeviceTier.valueOf(it.trim().uppercase()) }.getOrNull() }

        val resolvedClass = parsedClass ?: DeviceClass.UNKNOWN
        val resolvedTier = parsedTier ?: DeviceTier.TIER_2

        return DeviceInfo(
            deviceId = headers["X-Device-Id"],
            deviceClass = resolvedClass,
            tier = resolvedTier,
            appVersion = headers["X-App-Version"] ?: "unknown",
            platform = headers["X-Platform"] ?: "unknown",
            model = headers["X-Device-Model"],
            osVersion = headers["X-OS-Version"],
            policyVersion = headers["X-Policy-Version"]?.toIntOrNull()
        )
    }

    fun summary(call: ApplicationCall): String {
        val ctx = call.attributes.getOrNull(key)
        val info = ctx?.deviceInfo
        return "${call.request.path()} deviceClass=${info?.deviceClass} tier=${info?.tier}"
    }
}
