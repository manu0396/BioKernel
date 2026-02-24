package com.neogenesis.platform.gateway

import com.neogenesis.grpc.GatewayRunEvent
import com.neogenesis.grpc.GatewayServiceGrpcKt
import com.neogenesis.grpc.GatewayTelemetry
import com.neogenesis.grpc.HeartbeatRequest
import com.neogenesis.grpc.PushRunEventsRequest
import com.neogenesis.grpc.PushTelemetryRequest
import com.neogenesis.grpc.RegisterGatewayRequest
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Instant

object DeviceGateway {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val coreHost = System.getenv("CORE_GRPC_HOST") ?: "localhost"
        val corePort = (System.getenv("CORE_GRPC_PORT") ?: "9090").toInt()
        val useTls = (System.getenv("CORE_GRPC_TLS") ?: "false").toBoolean()
        val gatewayId = System.getenv("GATEWAY_ID") ?: "gateway-local"
        val runId = System.getenv("SIM_RUN_ID") ?: "run-simulated"

        val channel: ManagedChannel = NettyChannelBuilder.forAddress(coreHost, corePort)
            .apply { if (!useTls) usePlaintext() }
            .build()

        val stub = GatewayServiceGrpcKt.GatewayServiceCoroutineStub(channel)

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
