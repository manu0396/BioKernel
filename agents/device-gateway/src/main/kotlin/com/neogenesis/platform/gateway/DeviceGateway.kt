package com.neogenesis.platform.gateway

import com.neogenesis.platform.proto.v1.GatewayServiceGrpcKt
import com.neogenesis.platform.proto.v1.HeartbeatRequest
import com.neogenesis.platform.proto.v1.PushRunEventsRequest
import com.neogenesis.platform.proto.v1.PushTelemetryRequest
import com.neogenesis.platform.proto.v1.RegisterGatewayRequest
import com.neogenesis.platform.proto.v1.RunEvent
import com.neogenesis.platform.proto.v1.TelemetryFrame
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
                .setVersion("0.1.0")
                .build()
        )
        println("Registered gateway: ${registerResponse.status}")

        var tick = 0
        while (true) {
            val heartbeat = stub.heartbeat(
                HeartbeatRequest.newBuilder()
                    .setGatewayId(gatewayId)
                    .setStatus("OK")
                    .build()
            )
            println("Heartbeat: ${heartbeat.status}")

            val event = RunEvent.newBuilder()
                .setRunId(runId)
                .setEventType("GATEWAY_TICK")
                .setMessage("Tick $tick")
                .setCreatedAt(Instant.now().toString())
                .build()
            stub.pushRunEvents(
                PushRunEventsRequest.newBuilder()
                    .setGatewayId(gatewayId)
                    .setRunId(runId)
                    .addEvents(event)
                    .build()
            )

            val telemetry = TelemetryFrame.newBuilder()
                .setRunId(runId)
                .setTimestamp(Instant.now().toString())
                .setPressureKpa(110.0 + tick)
                .setFlowRate(4.0 + tick * 0.1)
                .build()
            stub.pushTelemetry(
                PushTelemetryRequest.newBuilder()
                    .setGatewayId(gatewayId)
                    .setRunId(runId)
                    .addFrames(telemetry)
                    .build()
            )

            tick += 1
            delay(5_000)
        }
    }
}
