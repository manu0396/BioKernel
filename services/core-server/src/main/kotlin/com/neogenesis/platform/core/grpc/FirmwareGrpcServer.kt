package com.neogenesis.platform.core.grpc

import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.ktor.server.application.*

class FirmwareGrpcServer(
    private val port: Int,
    private val services: List<io.grpc.BindableService>
) {
    private var server: Server? = null

    fun start() {
        server = NettyServerBuilder.forPort(port)
            .apply { services.forEach { addService(it) } }
            .build()
            .start()
    }

    fun stop() {
        server?.shutdown()
    }
}

fun Application.installGrpcServer(services: List<io.grpc.BindableService>, port: Int = 9090) {
    val server = FirmwareGrpcServer(port, services)
    environment.monitor.subscribe(ApplicationStarted) { server.start() }
    environment.monitor.subscribe(ApplicationStopped) { server.stop() }
}

