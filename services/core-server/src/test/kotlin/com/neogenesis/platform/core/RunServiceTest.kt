package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.RegenOpsRunService
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.grpc.GrpcDeviceContext
import com.neogenesis.grpc.GetRunRequest
import com.neogenesis.grpc.StartRunRequest
import com.neogenesis.platform.shared.domain.device.DeviceClass
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DeviceTier
import com.neogenesis.platform.shared.domain.device.effectiveCapabilities
import io.grpc.Context
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RunServiceTest {
    @Test
    fun startRunCreatesRun() = runBlocking {
        val service = RegenOpsRunService()
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
        val run = try {
            service.startRun(
                StartRunRequest.newBuilder().setProtocolId("proto-1").setProtocolVersion(1).build()
            )
        } finally {
            ctx.detach(prev)
        }
        val prevFetch = ctx.attach()
        val fetched = try {
            service.getRun(GetRunRequest.newBuilder().setRunId(run.runId).build())
        } finally {
            ctx.detach(prevFetch)
        }
        assertEquals(run.runId, fetched.runId)
    }
}

