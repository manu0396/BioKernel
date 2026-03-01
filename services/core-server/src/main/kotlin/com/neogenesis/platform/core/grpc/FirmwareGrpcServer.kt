package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.config.GrpcTlsConfig
import io.grpc.Server
import io.grpc.ServerInterceptor
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext
import io.ktor.server.application.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class FirmwareGrpcServer(
    private val port: Int,
    private val services: List<io.grpc.BindableService>,
    private val tlsConfig: GrpcTlsConfig?,
    private val interceptors: List<ServerInterceptor>
) {
    private val lock = Any()
    private var server: Server? = null
    private var reloader: ScheduledExecutorService? = null
    private var watcher: TlsFileWatcher? = null

    fun start() {
        synchronized(lock) {
            server = buildServer().start()
            startReloaderIfNeeded()
        }
    }

    fun stop() {
        synchronized(lock) {
            reloader?.shutdownNow()
            reloader = null
            watcher = null
            server?.shutdown()
        }
    }

    private fun buildServer(): Server {
        val builder = NettyServerBuilder.forPort(port)
        services.forEach { builder.addService(it) }
        interceptors.forEach { builder.intercept(it) }
        val tls = tlsConfig
        if (tls?.enabled == true) {
            builder.sslContext(buildSslContext(tls))
        }
        return builder.build()
    }

    private fun startReloaderIfNeeded() {
        val tls = tlsConfig
        if (tls?.enabled != true) return
        val paths = listOfNotNull(tls.certChainPath, tls.privateKeyPath, tls.trustCertPath)
        if (paths.isEmpty()) return
        watcher = TlsFileWatcher(paths)
        val intervalSeconds = tls.reloadIntervalSeconds
        if (intervalSeconds <= 0) return
        reloader = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "grpc-tls-reloader").apply { isDaemon = true }
        }
        reloader?.scheduleAtFixedRate({
            val changed = watcher?.hasChanges() ?: false
            if (changed) {
                synchronized(lock) {
                    server?.shutdown()
                    server = buildServer().start()
                }
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)
    }

    private fun buildSslContext(tls: GrpcTlsConfig): SslContext {
        val certChainPath = tls.certChainPath?.takeIf { it.isNotBlank() }
            ?: error("GRPC_TLS_CERT_CHAIN_PATH is required when TLS is enabled")
        val privateKeyPath = tls.privateKeyPath?.takeIf { it.isNotBlank() }
            ?: error("GRPC_TLS_PRIVATE_KEY_PATH is required when TLS is enabled")
        val builder = GrpcSslContexts.forServer(File(certChainPath), File(privateKeyPath))
        if (tls.requireClientAuth) {
            val trustCertPath = tls.trustCertPath?.takeIf { it.isNotBlank() }
                ?: error("GRPC_TLS_TRUST_CERT_PATH is required when client auth is enabled")
            builder.trustManager(File(trustCertPath))
            builder.clientAuth(ClientAuth.REQUIRE)
        }
        return builder.build()
    }
}

fun Application.installGrpcServer(
    services: List<io.grpc.BindableService>,
    port: Int = 9090,
    tlsConfig: GrpcTlsConfig? = null,
    interceptors: List<ServerInterceptor> = emptyList()
) {
    val server = FirmwareGrpcServer(port, services, tlsConfig, interceptors)
    environment.monitor.subscribe(ApplicationStarted) { server.start() }
    environment.monitor.subscribe(ApplicationStopped) { server.stop() }
}

class TlsFileWatcher(private val paths: List<String>) {
    private var lastModified: Map<String, Long> = paths.associateWith { File(it).lastModified() }

    fun hasChanges(): Boolean {
        val next = paths.associateWith { File(it).lastModified() }
        val changed = next.any { (path, time) -> time != (lastModified[path] ?: -1) }
        if (changed) {
            lastModified = next
        }
        return changed
    }
}
