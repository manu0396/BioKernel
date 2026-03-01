package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.proto.v1.DeviceClass
import com.neogenesis.platform.proto.v1.DeviceInfo
import com.neogenesis.platform.proto.v1.DevicePolicy
import com.neogenesis.platform.proto.v1.DevicePolicyServiceGrpcKt
import com.neogenesis.platform.proto.v1.Empty
import com.neogenesis.platform.proto.v1.TierCapabilities
import com.neogenesis.platform.proto.v1.ClassCapabilities
import com.neogenesis.platform.proto.v1.Capability
import com.neogenesis.platform.proto.v1.DeviceTier
import com.neogenesis.platform.shared.domain.device.DevicePolicy as DomainPolicy

class DevicePolicyGrpcService(
    private val repository: DevicePolicyRepository
) : DevicePolicyServiceGrpcKt.DevicePolicyServiceCoroutineImplBase() {
    override suspend fun getDevicePolicy(request: Empty): DevicePolicy = repository.load().toProto()

    override suspend fun registerDevice(request: DeviceInfo): DevicePolicy {
        // For now we only return policy; device registration can be persisted later.
        return repository.load().toProto()
    }
}

private fun DomainPolicy.toProto(): DevicePolicy {
    val builder = DevicePolicy.newBuilder()
        .setVersion(version)
        .setMinAppVersion(minAppVersion ?: "")
        .setAllowTier3Alerts(allowTier3Alerts ?: false)

    tierCaps?.forEach { (tier, caps) ->
        val entry = TierCapabilities.newBuilder()
            .setTier(DeviceTier.valueOf(tier.name))
            .addAllCapabilities(caps.map { Capability.valueOf(it.name) })
            .build()
        builder.addTierCaps(entry)
    }

    classCaps?.forEach { (cls, caps) ->
        val entry = ClassCapabilities.newBuilder()
            .setDeviceClass(DeviceClass.valueOf(cls.name))
            .addAllCapabilities(caps.map { Capability.valueOf(it.name) })
            .build()
        builder.addClassCaps(entry)
    }

    return builder.build()
}
