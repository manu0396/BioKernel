package com.neogenesis.platform.control.data.stream

import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.device.DeviceInfoStore
import com.neogenesis.platform.control.device.GrpcDeviceHeaders
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.CorrelationIds
import com.neogenesis.platform.shared.network.LogLevel
import com.neogenesis.platform.shared.network.TokenStorage
import com.neogenesis.platform.shared.telemetry.FlowRate
import com.neogenesis.platform.shared.telemetry.MPCPrediction
import com.neogenesis.platform.shared.telemetry.NozzleDisplacement
import com.neogenesis.platform.shared.telemetry.PIDState
import com.neogenesis.platform.shared.telemetry.PressureReading
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import com.neogenesis.platform.shared.telemetry.Temperature
import com.neogenesis.platform.shared.telemetry.ViscosityEstimation
import com.neogenesis.platform.proto.v1.GetRunRequest
import com.neogenesis.platform.proto.v1.RunServiceGrpcKt
import com.neogenesis.platform.proto.v1.RunEvent as ProtoRunEvent
import com.neogenesis.platform.proto.v1.TelemetryFrame as ProtoTelemetryFrame
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Instant

class GrpcRegenOpsStreamClient(
    private val config: AppConfig,
    private val tokenStorage: TokenStorage,
    private val logger: AppLogger,
    private val deviceInfoStore: DeviceInfoStore
) : RegenOpsStreamClient {
    private val channel: ManagedChannel = NettyChannelBuilder
        .forAddress(config.grpcHost, config.grpcPort)
        .apply { if (!config.grpcUseTls) usePlaintext() }
        .build()

    private fun runStub(correlationId: String): RunServiceGrpcKt.RunServiceCoroutineStub {
        return RunServiceGrpcKt.RunServiceCoroutineStub(channel)
            .withInterceptors(AuthMetadataInterceptor(tokenStorage, correlationId, deviceInfoStore))
    }

    override fun streamEvents(runId: String): Flow<RunEvent> = channelFlow {
        var attempt = 0
        while (isActive) {
            val correlationId = CorrelationIds.newId()
            try {
                val request = GetRunRequest.newBuilder().setRunId(runId).build()
                runStub(correlationId).streamRunEvents(request).collect { event ->
                    send(event.toDomain())
                }
                attempt = 0
            } catch (ex: Exception) {
                val backoffMs = backoffDelay(attempt)
                logger.log(LogLevel.WARN, "gRPC events stream disconnected", mapOf("correlation_id" to correlationId))
                delay(backoffMs)
                attempt += 1
            }
        }
    }

    override fun streamTelemetry(runId: String): Flow<TelemetryFrame> = channelFlow {
        var attempt = 0
        while (isActive) {
            val correlationId = CorrelationIds.newId()
            try {
                val request = GetRunRequest.newBuilder().setRunId(runId).build()
                runStub(correlationId).streamTelemetry(request).collect { frame ->
                    send(frame.toDomain())
                }
                attempt = 0
            } catch (ex: Exception) {
                val backoffMs = backoffDelay(attempt)
                logger.log(LogLevel.WARN, "gRPC telemetry stream disconnected", mapOf("correlation_id" to correlationId))
                delay(backoffMs)
                attempt += 1
            }
        }
    }

    override fun close() {
        channel.shutdownNow()
    }
}

private fun backoffDelay(attempt: Int): Long {
    val base = 1000L
    val max = 30_000L
    val exp = base * (1 shl attempt.coerceAtMost(5))
    val jitter = (0..250).random()
    return (exp + jitter).coerceAtMost(max)
}

private class AuthMetadataInterceptor(
    private val tokenStorage: TokenStorage,
    private val correlationId: String,
    private val deviceInfoStore: DeviceInfoStore
) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val call = next.newCall(method, callOptions)
        return object : ClientCall<ReqT, RespT>() {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                val auth = tokenStorage.readAccessToken()
                if (!auth.isNullOrBlank()) {
                    headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer $auth")
                }
                headers.put(Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER), correlationId)
                GrpcDeviceHeaders.apply(headers, deviceInfoStore.get())
                call.start(responseListener, headers)
            }

            override fun request(numMessages: Int) = call.request(numMessages)
            override fun cancel(message: String?, cause: Throwable?) = call.cancel(message, cause)
            override fun halfClose() = call.halfClose()
            override fun sendMessage(message: ReqT) = call.sendMessage(message)
        }
    }
}

private fun ProtoRunEvent.toDomain(): RunEvent {
    return RunEvent(
        id = "${runId}-${createdAt}",
        runId = RunId(runId),
        eventType = eventType,
        message = message,
        createdAt = runCatching { Instant.parse(createdAt) }.getOrElse { Instant.fromEpochMilliseconds(0) }
    )
}

private fun ProtoTelemetryFrame.toDomain(): TelemetryFrame {
    return TelemetryFrame(
        timestamp = Instant.fromEpochMilliseconds(timestampMs),
        pressure = PressureReading(pressureKpa),
        displacement = NozzleDisplacement(displacementUm),
        flowRate = FlowRate(flowRateUlS),
        temperature = Temperature(temperatureC),
        viscosity = ViscosityEstimation(viscosityPas),
        pid = PIDState(pidP, pidI, pidD),
        mpc = MPCPrediction(mpcHorizonMs, mpcPredictedPressureKpa)
    )
}
