package com.neogenesis.platform.control.device

import com.neogenesis.platform.shared.domain.device.DeviceInfo
import io.grpc.Metadata

object GrpcDeviceHeaders {
    fun apply(headers: Metadata, info: DeviceInfo) {
        headers.put(DEVICE_ID, info.deviceId.orEmpty())
        headers.put(DEVICE_CLASS, info.deviceClass.name)
        headers.put(DEVICE_TIER, info.tier.name)
        headers.put(APP_VERSION, info.appVersion)
        headers.put(PLATFORM, info.platform)
        info.osVersion?.let { headers.put(OS_VERSION, it) }
        info.model?.let { headers.put(DEVICE_MODEL, it) }
        info.policyVersion?.let { headers.put(POLICY_VERSION, it.toString()) }
    }

    private val DEVICE_ID: Metadata.Key<String> =
        Metadata.Key.of("x-device-id", Metadata.ASCII_STRING_MARSHALLER)
    private val DEVICE_CLASS: Metadata.Key<String> =
        Metadata.Key.of("x-device-class", Metadata.ASCII_STRING_MARSHALLER)
    private val DEVICE_TIER: Metadata.Key<String> =
        Metadata.Key.of("x-device-tier", Metadata.ASCII_STRING_MARSHALLER)
    private val APP_VERSION: Metadata.Key<String> =
        Metadata.Key.of("x-app-version", Metadata.ASCII_STRING_MARSHALLER)
    private val PLATFORM: Metadata.Key<String> =
        Metadata.Key.of("x-platform", Metadata.ASCII_STRING_MARSHALLER)
    private val OS_VERSION: Metadata.Key<String> =
        Metadata.Key.of("x-os-version", Metadata.ASCII_STRING_MARSHALLER)
    private val DEVICE_MODEL: Metadata.Key<String> =
        Metadata.Key.of("x-device-model", Metadata.ASCII_STRING_MARSHALLER)
    private val POLICY_VERSION: Metadata.Key<String> =
        Metadata.Key.of("x-policy-version", Metadata.ASCII_STRING_MARSHALLER)
}
