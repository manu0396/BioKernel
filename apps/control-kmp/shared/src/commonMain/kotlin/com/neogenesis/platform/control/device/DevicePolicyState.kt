package com.neogenesis.platform.control.device

import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DevicePolicy
import com.neogenesis.platform.shared.domain.device.effectiveCapabilities

data class DevicePolicyState(
    val deviceInfo: DeviceInfo,
    val policy: DevicePolicy?,
    val effectiveCapabilities: Set<Capability>
)

class DeviceInfoStore(initial: DeviceInfo) {
    @Volatile
    private var currentInfo: DeviceInfo = initial

    fun get(): DeviceInfo = currentInfo

    fun updatePolicyVersion(version: Int?) {
        currentInfo = currentInfo.copy(policyVersion = version)
    }
}

fun buildDevicePolicyState(deviceInfo: DeviceInfo, policy: DevicePolicy?): DevicePolicyState {
    val caps = effectiveCapabilities(deviceInfo.tier, deviceInfo.deviceClass, policy)
    return DevicePolicyState(deviceInfo = deviceInfo, policy = policy, effectiveCapabilities = caps)
}
