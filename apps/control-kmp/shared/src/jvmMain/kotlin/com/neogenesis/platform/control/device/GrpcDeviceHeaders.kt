package com.neogenesis.platform.control.device

import com.neogenesis.platform.shared.domain.device.DeviceInfo
import io.grpc.Metadata

object GrpcDeviceHeaders {
    private val deviceId = Metadata.Key.of("x-device-id", Metadata.ASCII_STRING_MARSHALLER)
    private val deviceClass = Metadata.Key.of("x-device-class", Metadata.ASCII_STRING_MARSHALLER)
    private val deviceTier = Metadata.Key.of("x-device-tier", Metadata.ASCII_STRING_MARSHALLER)
    private val appVersion = Metadata.Key.of("x-app-version", Metadata.ASCII_STRING_MARSHALLER)
    private val platform = Metadata.Key.of("x-platform", Metadata.ASCII_STRING_MARSHALLER)
    private val osVersion = Metadata.Key.of("x-os-version", Metadata.ASCII_STRING_MARSHALLER)
    private val deviceModel = Metadata.Key.of("x-device-model", Metadata.ASCII_STRING_MARSHALLER)
    private val policyVersion = Metadata.Key.of("x-policy-version", Metadata.ASCII_STRING_MARSHALLER)

    fun apply(headers: Metadata, info: DeviceInfo) {
        info.deviceId?.let { headers.put(deviceId, it) }
        headers.put(deviceClass, info.deviceClass.name)
        headers.put(deviceTier, info.tier.name)
        headers.put(appVersion, info.appVersion)
        headers.put(platform, info.platform)
        info.osVersion?.let { headers.put(osVersion, it) }
        info.model?.let { headers.put(deviceModel, it) }
        info.policyVersion?.let { headers.put(policyVersion, it.toString()) }
    }
}
