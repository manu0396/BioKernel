package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.RegenOpsProtocolService
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.grpc.GrpcDeviceContext
import com.neogenesis.grpc.ListProtocolsRequest
import com.neogenesis.platform.shared.domain.device.DeviceClass
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DeviceTier
import com.neogenesis.platform.shared.domain.device.effectiveCapabilities
import io.grpc.Context
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ProtocolServiceTest {
    @Test
    fun listProtocolsReturnsSeed() = runBlocking {
        val service = RegenOpsProtocolService()
        val policyRepository = DevicePolicyRepository()
        val policy = policyRepository.load()
        val info = DeviceInfo(
            deviceId = "00000000-0000-0000-0000-000000000002",
            deviceClass = DeviceClass.WINDOWS_DESKTOP,
            tier = DeviceTier.TIER_1,
            appVersion = "1.0.0",
            platform = "test",
            model = null,
            osVersion = null,
            policyVersion = policy?.version
        )
        val caps = effectiveCapabilities(info.tier, info.deviceClass, policy)
        val ctx = Context.current()
            .withValue(GrpcDeviceContext.deviceInfoKey, info)
            .withValue(GrpcDeviceContext.capsKey, caps)
            .withValue(GrpcDeviceContext.methodKey, "test")
        val prev = ctx.attach()
        val response = try {
            service.listProtocols(ListProtocolsRequest.newBuilder().build())
        } finally {
            ctx.detach(prev)
        }
        assertTrue(response.protocolsCount >= 1)
    }
}

