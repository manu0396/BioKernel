package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.TelemetryBus
import com.neogenesis.platform.core.grpc.TelemetryStreamServiceImpl
import com.neogenesis.platform.proto.v1.TelemetryRequest
import com.neogenesis.platform.proto.v1.TelemetryStreamServiceGrpcKt
import com.neogenesis.platform.shared.telemetry.*
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class GrpcTelemetryStreamTest {
    @Test
    fun telemetryStreamDeliversFrames() = runBlocking {
        val name = InProcessServerBuilder.generateName()
        val bus = TelemetryBus()
        val server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(TelemetryStreamServiceImpl(bus))
            .build()
            .start()
        val channel = InProcessChannelBuilder.forName(name).directExecutor().build()

        try {
            val stub = TelemetryStreamServiceGrpcKt.TelemetryStreamServiceCoroutineStub(channel)
            val requestFlow = flow {
                emit(
                    TelemetryRequest.newBuilder()
                        .setJobId("job")
                        .setDeviceId("device")
                        .setRateMs(1)
                        .build()
                )
                awaitCancellation()
            }
            val response = async {
                withTimeout(10_000) { stub.streamTelemetry(requestFlow).first() }
            }
            delay(200)
            bus.emit(
                TelemetryFrame(
                    timestamp = Clock.System.now(),
                    pressure = PressureReading(120.0),
                    displacement = NozzleDisplacement(1.0),
                    flowRate = FlowRate(5.0),
                    temperature = Temperature(30.0),
                    viscosity = ViscosityEstimation(1.1),
                    pid = PIDState(1.0, 0.1, 0.01),
                    mpc = MPCPrediction(50, 118.0)
                )
            )
            val frame = response.await()
            assertEquals("job", frame.jobId)
            assertEquals("device", frame.deviceId)
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }

    @Test
    fun telemetryStreamNeverUsesUnknownIdsAfterStartRace() = runBlocking {
        val name = InProcessServerBuilder.generateName()
        val bus = TelemetryBus()
        val server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(TelemetryStreamServiceImpl(bus))
            .build()
            .start()
        val channel = InProcessChannelBuilder.forName(name).directExecutor().build()

        try {
            val stub = TelemetryStreamServiceGrpcKt.TelemetryStreamServiceCoroutineStub(channel)

            repeat(25) { index ->
                val requestFlow = flow {
                    emit(
                        TelemetryRequest.newBuilder()
                            .setJobId("job-$index")
                            .setDeviceId("device-$index")
                            .setRateMs(1)
                            .build()
                    )
                    awaitCancellation()
                }

                val response = async {
                    withTimeout(10_000) { stub.streamTelemetry(requestFlow).first() }
                }

                // Emit immediately to stress request/stream startup ordering.
                launch {
                    bus.emit(
                        TelemetryFrame(
                            timestamp = Clock.System.now(),
                            pressure = PressureReading(120.0),
                            displacement = NozzleDisplacement(1.0),
                            flowRate = FlowRate(5.0),
                            temperature = Temperature(30.0),
                            viscosity = ViscosityEstimation(1.1),
                            pid = PIDState(1.0, 0.1, 0.01),
                            mpc = MPCPrediction(50, 118.0)
                        )
                    )
                }

                val frame = response.await()
                assertEquals("job-$index", frame.jobId)
                assertEquals("device-$index", frame.deviceId)
            }
        } finally {
            channel.shutdownNow()
            server.shutdownNow()
        }
    }
}

