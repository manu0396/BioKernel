package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.RegenOpsRunService
import com.neogenesis.platform.core.grpc.TlsFileWatcher
import com.neogenesis.grpc.RunServiceGrpcKt
import com.neogenesis.grpc.StartRunRequest
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.netty.shaded.io.netty.handler.ssl.util.SelfSignedCertificate
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

        val server = NettyServerBuilder.forPort(0)
            .sslContext(serverContext)
            .addService(RegenOpsRunService())
            .build()
            .start()
        val channel = NettyChannelBuilder.forAddress("localhost", server.port)
            .sslContext(clientContext)
            .build()

        try {
            val response = RunServiceGrpcKt.RunServiceCoroutineStub(channel).startRun(
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
}
