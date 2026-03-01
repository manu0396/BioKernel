package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.storage.TelemetryRepositoryImpl
import com.neogenesis.platform.core.storage.DeviceRepositoryImpl
import com.neogenesis.platform.core.telemetry.TelemetryCheckpointTracker
import com.neogenesis.platform.firmware.v1.CommandRequest
import com.neogenesis.platform.firmware.v1.CommandResponse
import com.neogenesis.platform.firmware.v1.DeviceHealth
import com.neogenesis.platform.firmware.v1.FirmwareBridgeGrpcKt
import com.neogenesis.platform.firmware.v1.TelemetryFrame
import com.neogenesis.platform.shared.domain.device.Capability
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FirmwareBridgeService(
    private val telemetryRepository: TelemetryRepositoryImpl,
    private val deviceRepository: DeviceRepositoryImpl,
    private val telemetryBus: TelemetryBus,
    private val commandBus: DeviceCommandBus,
    private val checkpointTracker: TelemetryCheckpointTracker
) : FirmwareBridgeGrpcKt.FirmwareBridgeCoroutineImplBase() {

    override fun telemetryStream(requests: Flow<TelemetryFrame>): Flow<CommandRequest> = channelFlow {
        GrpcCapabilityGuard.requireCapability(Capability.LIVE_MONITOR)
        val watchdog = TelemetryWatchdog(timeoutMs = 500)
        launch {
            while (true) {
                delay(250)
                val now = System.currentTimeMillis()
                if (watchdog.isExpired(now)) {
                    send(
                        CommandRequest.newBuilder()
                            .setCommandId("cmd-emergency-$now")
                            .setDeviceId("unknown")
                            .setCommand("EMERGENCY_STOP")
                            .setParametersJson("{\"reason\":\"telemetry_timeout\"}")
                            .build()
                    )
                }
            }
        }
        launch {
            commandBus.stream().collect { cmd ->
                send(
                    CommandRequest.newBuilder()
                        .setCommandId(cmd.commandId)
                        .setDeviceId(cmd.deviceId)
                        .setCommand(cmd.command)
                        .setParametersJson(cmd.parametersJson)
                        .build()
                )
            }
        }
        requests.collect { frame ->
            watchdog.mark(frame.timestampMs)
            telemetryRepository.store(frame)
            val mapped = TelemetryMapper.fromFirmware(frame)
            telemetryBus.emit(mapped)
            checkpointTracker.record(
                com.neogenesis.platform.shared.domain.PrintJobId(frame.jobId),
                com.neogenesis.platform.shared.domain.DeviceId(frame.deviceId)
            )
            if (frame.pressureKpa > 250.0) {
                send(
                    CommandRequest.newBuilder()
                        .setCommandId("cmd-${frame.timestampMs}")
                        .setDeviceId("unknown")
                        .setCommand("ADJUST_PRESSURE")
                        .setParametersJson("{\"targetKpa\":200}")
                        .build()
                )
            }
        }
    }

    override suspend fun healthStream(requests: Flow<DeviceHealth>): CommandResponse {
        GrpcCapabilityGuard.requireCapability(Capability.LIVE_MONITOR)
        var last: DeviceHealth? = null
        requests.collect { health ->
            last = health
            deviceRepository.recordHealth(
                com.neogenesis.platform.shared.domain.DeviceId(health.deviceId),
                health.status,
                kotlinx.datetime.Instant.fromEpochMilliseconds(health.timestampMs)
            )
        }
        return CommandResponse.newBuilder()
            .setCommandId("health-ack")
            .setDeviceId(last?.deviceId ?: "unknown")
            .setAccepted(true)
            .setMessage("health received")
            .build()
    }
}

