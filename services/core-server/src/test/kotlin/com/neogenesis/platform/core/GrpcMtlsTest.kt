package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.RegenOpsRunService
import com.neogenesis.platform.core.grpc.TlsFileWatcher
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.grpc.GrpcDeviceContext
import com.neogenesis.grpc.RunServiceGrpcKt
import com.neogenesis.grpc.StartRunRequest
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.netty.shaded.io.netty.handler.ssl.util.SelfSignedCertificate
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrpcMtlsTest {
    @Test
    fun mtlsHandshakeSucceeds() = runBlocking {
        val serverCert = SelfSignedCertificate()
        val clientCert = SelfSignedCertificate()
        val serverContext = GrpcSslContexts.forServer(serverCert.certificate(), serverCert.privateKey())
            .trustManager(clientCert.certificate())
            .clientAuth(ClientAuth.REQUIRE)
            .build()
        val clientContext = GrpcSslContexts.forClient()
            .keyManager(clientCert.certificate(), clientCert.privateKey())
            .trustManager(serverCert.certificate())
            .build()

        val policyRepository = DevicePolicyRepository()
        val server = NettyServerBuilder.forPort(0)
            .sslContext(serverContext)
            .addService(
                io.grpc.ServerInterceptors.intercept(
                    RegenOpsRunService(),
                    GrpcDeviceContext.interceptor(policyRepository)
                )
            )
            .build()
            .start()
        val channel = NettyChannelBuilder.forAddress("localhost", server.port)
            .sslContext(clientContext)
            .build()

        try {
            val response = RunServiceGrpcKt.RunServiceCoroutineStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(deviceHeaders()))
                .startRun(
                StartRunRequest.newBuilder()
                    .setProtocolId("proto-1")
                    .setProtocolVersion(1)
                    .setRunId("run-mtls")
                    .setGatewayId("gateway-test")
                    .build()
            )
            assertEquals("RUNNING", response.status)
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }

    @Test
    fun tlsWatcherDetectsFileChanges() {
        val certFile = File.createTempFile("cert", ".pem")
        certFile.writeText("initial")
        val watcher = TlsFileWatcher(listOf(certFile.absolutePath))
        assertFalse(watcher.hasChanges())
        certFile.writeText("updated")
        assertTrue(watcher.hasChanges())
    }

    private fun deviceHeaders(): Metadata {
        val metadata = Metadata()
        metadata.put(Metadata.Key.of("x-device-class", Metadata.ASCII_STRING_MARSHALLER), "WINDOWS_DESKTOP")
        metadata.put(Metadata.Key.of("x-device-tier", Metadata.ASCII_STRING_MARSHALLER), "TIER_1")
        metadata.put(Metadata.Key.of("x-app-version", Metadata.ASCII_STRING_MARSHALLER), "1.0.0")
        metadata.put(Metadata.Key.of("x-platform", Metadata.ASCII_STRING_MARSHALLER), "desktop")
        metadata.put(Metadata.Key.of("x-device-id", Metadata.ASCII_STRING_MARSHALLER), "00000000-0000-0000-0000-000000000002")
        return metadata
    }
}

