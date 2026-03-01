package com.neogenesis.platform.core

import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.grpc.GrpcDeviceContext
import com.neogenesis.platform.core.grpc.RegenOpsRunService
import com.neogenesis.platform.proto.v1.RunServiceGrpcKt
import com.neogenesis.platform.proto.v1.StartRunRequest
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class DeviceTierGrpcEnforcementTest {
    @Test
    fun tier2DeniedTier1Allowed() = runBlocking {
        val name = InProcessServerBuilder.generateName()
        val policyRepository = DevicePolicyRepository()
        val service = RegenOpsRunService()
        val server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                io.grpc.ServerInterceptors.intercept(
                    service,
                    GrpcDeviceContext.interceptor(policyRepository)
                )
            )
            .build()
            .start()

        val channel = InProcessChannelBuilder.forName(name).directExecutor().build()

        val tier2Stub = RunServiceGrpcKt.RunServiceCoroutineStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(deviceHeaders("ANDROID_TABLET", "TIER_2")))

        val tier1Stub = RunServiceGrpcKt.RunServiceCoroutineStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(deviceHeaders("WINDOWS_DESKTOP", "TIER_1")))

        val denied = assertFailsWith<io.grpc.StatusRuntimeException> {
            tier2Stub.startRun(
                StartRunRequest.newBuilder()
                    .setProtocolId("proto-1")
                    .setProtocolVersion(1)
                    .build()
            )
        }
        assertEquals(Status.Code.PERMISSION_DENIED, denied.status.code)

        val allowed = tier1Stub.startRun(
            StartRunRequest.newBuilder()
                .setProtocolId("proto-1")
                .setProtocolVersion(1)
                .build()
        )
        assertNotNull(allowed.runId)

        channel.shutdownNow()
        server.shutdownNow()
    }

    private fun deviceHeaders(deviceClass: String, tier: String): Metadata {
        val metadata = Metadata()
        metadata.put(Metadata.Key.of("x-device-class", Metadata.ASCII_STRING_MARSHALLER), deviceClass)
        metadata.put(Metadata.Key.of("x-device-tier", Metadata.ASCII_STRING_MARSHALLER), tier)
        metadata.put(Metadata.Key.of("x-app-version", Metadata.ASCII_STRING_MARSHALLER), "1.0.0")
        metadata.put(Metadata.Key.of("x-platform", Metadata.ASCII_STRING_MARSHALLER), "desktop")
        metadata.put(Metadata.Key.of("x-device-id", Metadata.ASCII_STRING_MARSHALLER), "test-device")
        return metadata
    }
}
