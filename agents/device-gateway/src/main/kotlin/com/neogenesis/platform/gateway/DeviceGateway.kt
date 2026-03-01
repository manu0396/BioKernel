package com.neogenesis.platform.gateway

import com.neogenesis.grpc.GatewayRunEvent
import com.neogenesis.grpc.GatewayServiceGrpcKt
import com.neogenesis.grpc.GatewayTelemetry
import com.neogenesis.grpc.HeartbeatRequest
import com.neogenesis.grpc.PushRunEventsRequest
import com.neogenesis.grpc.PushTelemetryRequest
import com.neogenesis.grpc.RegisterGatewayRequest
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant

object DeviceGateway {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val coreHost = System.getenv("CORE_GRPC_HOST") ?: "localhost"
        val corePort = (System.getenv("CORE_GRPC_PORT") ?: "9090").toInt()
        val useTls = (System.getenv("CORE_GRPC_TLS") ?: "false").toBoolean()
        val clientCert = System.getenv("CORE_GRPC_TLS_CERT_CHAIN_PATH")
        val clientKey = System.getenv("CORE_GRPC_TLS_PRIVATE_KEY_PATH")
        val trustCert = System.getenv("CORE_GRPC_TLS_TRUST_CERT_PATH")
        val gatewayId = System.getenv("GATEWAY_ID") ?: "gateway-local"
        val runId = System.getenv("SIM_RUN_ID") ?: "run-simulated"
        val appVersion = System.getenv("GATEWAY_APP_VERSION") ?: "1.0.0"

        val channelBuilder = NettyChannelBuilder.forAddress(coreHost, corePort)
        if (!useTls) {
            channelBuilder.usePlaintext()
        } else {
            val sslBuilder = GrpcSslContexts.forClient()
            if (!trustCert.isNullOrBlank()) {
                sslBuilder.trustManager(File(trustCert))
            }
            if (!clientCert.isNullOrBlank() && !clientKey.isNullOrBlank()) {
                sslBuilder.keyManager(File(clientCert), File(clientKey))
            }
            channelBuilder.sslContext(sslBuilder.build())
        }
        val channel: ManagedChannel = channelBuilder.build()

        val stub = GatewayServiceGrpcKt.GatewayServiceCoroutineStub(channel)
            .withInterceptors(DeviceMetadataInterceptor(gatewayId, appVersion))

        val registerResponse = stub.registerGateway(
            RegisterGatewayRequest.newBuilder()
                .setGatewayId(gatewayId)
                .setDisplayName("Gateway $gatewayId")
                .build()
        )
        println("Registered gateway: ${registerResponse.status}")

        var tick = 0
        while (true) {
            val heartbeat = stub.heartbeat(
                HeartbeatRequest.newBuilder()
                    .setGatewayId(gatewayId)
                    .build()
            )
            println("Heartbeat: ${heartbeat.status}")

            val event = GatewayRunEvent.newBuilder()
                .setRunId(runId)
                .setEventType("GATEWAY_TICK")
                .setPayloadJson("""{"message":"Tick $tick"}""")
                .setCreatedAtMs(Instant.now().toEpochMilli())
                .setSeq(tick.toLong())
                .build()
            stub.pushRunEvents(
                PushRunEventsRequest.newBuilder()
                    .setGatewayId(gatewayId)
                    .addEvents(event)
                    .build()
            )

            val pressureTelemetry = GatewayTelemetry.newBuilder()
                .setRunId(runId)
                .setMetricKey("pressure_kpa")
                .setMetricValue(110.0 + tick)
                .setUnit("kPa")
                .setRecordedAtMs(Instant.now().toEpochMilli())
                .setSeq(tick.toLong())
                .build()
            val flowTelemetry = GatewayTelemetry.newBuilder()
                .setRunId(runId)
                .setMetricKey("flow_rate")
                .setMetricValue(4.0 + tick * 0.1)
                .setUnit("l/min")
                .setRecordedAtMs(Instant.now().toEpochMilli())
                .setSeq(tick.toLong())
                .build()
            stub.pushTelemetry(
                PushTelemetryRequest.newBuilder()
                    .setGatewayId(gatewayId)
                    .addTelemetry(pressureTelemetry)
                    .addTelemetry(flowTelemetry)
                    .build()
            )

            tick += 1
            delay(5_000)
        }
    }
}

private class DeviceMetadataInterceptor(
    private val deviceId: String,
    private val appVersion: String
) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val call = next.newCall(method, callOptions)
        return object : ClientCall<ReqT, RespT>() {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                headers.put(Metadata.Key.of("x-device-id", Metadata.ASCII_STRING_MARSHALLER), deviceId)
                headers.put(Metadata.Key.of("x-device-class", Metadata.ASCII_STRING_MARSHALLER), "EMBEDDED_TOUCHSCREEN")
                headers.put(Metadata.Key.of("x-device-tier", Metadata.ASCII_STRING_MARSHALLER), "TIER_1")
                headers.put(Metadata.Key.of("x-app-version", Metadata.ASCII_STRING_MARSHALLER), appVersion)
                headers.put(Metadata.Key.of("x-platform", Metadata.ASCII_STRING_MARSHALLER), "embedded")
                call.start(responseListener, headers)
            }

            override fun request(numMessages: Int) = call.request(numMessages)
            override fun cancel(message: String?, cause: Throwable?) = call.cancel(message, cause)
            override fun halfClose() = call.halfClose()
            override fun sendMessage(message: ReqT) = call.sendMessage(message)
        }
    }
}
