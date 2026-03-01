package com.neogenesis.platform.shared.network

import com.neogenesis.platform.shared.domain.device.DeviceInfo

object DeviceHeaders {
    const val DEVICE_ID = "X-Device-Id"
    const val DEVICE_CLASS = "X-Device-Class"
    const val DEVICE_TIER = "X-Device-Tier"
    const val APP_VERSION = "X-App-Version"
    const val PLATFORM = "X-Platform"
    const val OS_VERSION = "X-OS-Version"
    const val DEVICE_MODEL = "X-Device-Model"
    const val POLICY_VERSION = "X-Policy-Version"
}

fun MutableMap<String, String>.putDeviceHeaders(info: DeviceInfo) {
    info.deviceId?.let { put(DeviceHeaders.DEVICE_ID, it) }
    put(DeviceHeaders.DEVICE_CLASS, info.deviceClass.name)
    put(DeviceHeaders.DEVICE_TIER, info.tier.name)
    put(DeviceHeaders.APP_VERSION, info.appVersion)
    put(DeviceHeaders.PLATFORM, info.platform)
    info.osVersion?.let { put(DeviceHeaders.OS_VERSION, it) }
    info.model?.let { put(DeviceHeaders.DEVICE_MODEL, it) }
    info.policyVersion?.let { put(DeviceHeaders.POLICY_VERSION, it.toString()) }
}
