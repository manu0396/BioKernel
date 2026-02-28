package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.storage.DevicePairingRepositoryImpl
import com.neogenesis.platform.proto.v1.*
import com.neogenesis.platform.proto.v1.DeviceControlServiceGrpcKt.DeviceControlServiceCoroutineImplBase
import com.neogenesis.platform.proto.v1.PairingServiceGrpcKt.PairingServiceCoroutineImplBase
import com.neogenesis.platform.proto.v1.PrintJobEventServiceGrpcKt.PrintJobEventServiceCoroutineImplBase
import com.neogenesis.platform.proto.v1.TelemetryStreamServiceGrpcKt.TelemetryStreamServiceCoroutineImplBase
import com.neogenesis.platform.shared.security.Crypto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.util.UUID

class TelemetryStreamServiceImpl(
    private val telemetryBus: TelemetryBus
) : TelemetryStreamServiceCoroutineImplBase() {
    override fun streamTelemetry(requests: Flow<TelemetryRequest>): Flow<TelemetryFrame> = channelFlow {
        var rateMs = 10L
        var jobId = "unknown"
        var deviceId = "unknown"
        val initialized = CompletableDeferred<Unit>()

        launch {
            requests.collect { req ->
                if (req.rateMs > 0) rateMs = req.rateMs.toLong()
                if (req.jobId.isNotBlank()) jobId = req.jobId
                if (req.deviceId.isNotBlank()) deviceId = req.deviceId
                if (!initialized.isCompleted) {
                    initialized.complete(Unit)
                }
            }
        }
        launch {
            initialized.await()
            telemetryBus.stream().collect { frame ->
                send(
                    TelemetryMapper.toProto(
                        jobId = jobId,
                        deviceId = deviceId,
                        frame = frame
                    )
                )
                delay(rateMs)
            }
        }
    }
}

class DeviceControlServiceImpl(
    private val commandBus: DeviceCommandBus
) : DeviceControlServiceCoroutineImplBase() {
    override fun control(requests: Flow<DeviceControlCommand>): Flow<DeviceControlAck> = channelFlow {
        requests.collect { cmd ->
            commandBus.emit(cmd)
            send(
                DeviceControlAck.newBuilder()
                    .setCommandId(cmd.commandId)
                    .setDeviceId(cmd.deviceId)
                    .setAccepted(true)
                    .setMessage("accepted")
                    .build()
            )
        }
    }
}

class PrintJobEventServiceImpl(
    private val eventBus: PrintJobEventBus
) : PrintJobEventServiceCoroutineImplBase() {
    override fun streamEvents(requests: Flow<PrintJobEvent>): Flow<PrintJobEvent> = channelFlow {
        launch {
            requests.collect { event ->
                eventBus.emit(event)
            }
        }
        launch {
            eventBus.stream().collect { event ->
                send(event)
            }
        }
    }
}

class PairingServiceImpl(
    private val pairingRepository: DevicePairingRepositoryImpl,
    private val pairingSecret: String
) : PairingServiceCoroutineImplBase() {
    override suspend fun startPairing(request: PairingChallenge): PairingResult {
        val pairingId = if (request.pairingId.isBlank()) UUID.randomUUID().toString() else request.pairingId
        val challenge = if (request.challenge.isBlank()) UUID.randomUUID().toString() else request.challenge
        pairingRepository.create(
            com.neogenesis.platform.shared.domain.DevicePairing(
                id = com.neogenesis.platform.shared.domain.DevicePairingId(pairingId),
                deviceId = com.neogenesis.platform.shared.domain.DeviceId(request.deviceId),
                challenge = challenge,
                response = null,
                status = com.neogenesis.platform.shared.domain.PairingStatus.PENDING,
                createdAt = Clock.System.now(),
                completedAt = null
            )
        )
        return PairingResult.newBuilder()
            .setPairingId(pairingId)
            .setDeviceId(request.deviceId)
            .setVerified(false)
            .setMessage(challenge)
            .build()
    }

    override suspend fun completePairing(request: PairingResponse): PairingResult {
        val pairing = pairingRepository.findById(com.neogenesis.platform.shared.domain.DevicePairingId(request.pairingId))
            ?: return PairingResult.newBuilder()
                .setPairingId(request.pairingId)
                .setDeviceId(request.deviceId)
                .setVerified(false)
                .setMessage("pairing not found")
                .build()
        val expected = Crypto.hmacSha256(pairingSecret, pairing.challenge)
        val verified = expected == request.response
        pairingRepository.complete(
            com.neogenesis.platform.shared.domain.DevicePairingId(request.pairingId),
            request.response,
            if (verified) com.neogenesis.platform.shared.domain.PairingStatus.VERIFIED else com.neogenesis.platform.shared.domain.PairingStatus.FAILED,
            Clock.System.now()
        )
        return PairingResult.newBuilder()
            .setPairingId(request.pairingId)
            .setDeviceId(request.deviceId)
            .setVerified(verified)
            .setMessage(if (verified) "verified" else "invalid response")
            .build()
    }
}

