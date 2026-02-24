package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.firmware.v1.TelemetryFrame as FirmwareTelemetry
import com.neogenesis.platform.proto.v1.TelemetryFrame as ProtoTelemetry
import com.neogenesis.platform.shared.telemetry.*
import kotlinx.datetime.Instant

object TelemetryMapper {
    fun fromProto(frame: ProtoTelemetry): TelemetryFrame = TelemetryFrame(
        timestamp = Instant.fromEpochMilliseconds(frame.timestampMs),
        pressure = PressureReading(frame.pressureKpa),
        displacement = NozzleDisplacement(frame.displacementUm),
        flowRate = FlowRate(frame.flowRateUlS),
        temperature = Temperature(frame.temperatureC),
        viscosity = ViscosityEstimation(frame.viscosityPas),
        pid = PIDState(frame.pidP, frame.pidI, frame.pidD),
        mpc = MPCPrediction(frame.mpcHorizonMs, frame.mpcPredictedPressureKpa)
    )

    fun fromFirmware(frame: FirmwareTelemetry): TelemetryFrame = TelemetryFrame(
        timestamp = Instant.fromEpochMilliseconds(frame.timestampMs),
        pressure = PressureReading(frame.pressureKpa),
        displacement = NozzleDisplacement(frame.displacementUm),
        flowRate = FlowRate(frame.flowRateUlS),
        temperature = Temperature(frame.temperatureC),
        viscosity = ViscosityEstimation(frame.viscosityPas),
        pid = PIDState(frame.pidP, frame.pidI, frame.pidD),
        mpc = MPCPrediction(frame.mpcHorizonMs, frame.mpcPredictedPressureKpa)
    )

    fun toProto(jobId: String, deviceId: String, frame: TelemetryFrame): ProtoTelemetry =
        ProtoTelemetry.newBuilder()
            .setJobId(jobId)
            .setDeviceId(deviceId)
            .setTimestampMs(frame.timestamp.toEpochMilliseconds())
            .setPressureKpa(frame.pressure.kpa)
            .setDisplacementUm(frame.displacement.micrometers)
            .setFlowRateUlS(frame.flowRate.microlitersPerSecond)
            .setTemperatureC(frame.temperature.celsius)
            .setViscosityPas(frame.viscosity.pascalSecond)
            .setPidP(frame.pid.proportional)
            .setPidI(frame.pid.integral)
            .setPidD(frame.pid.derivative)
            .setMpcHorizonMs(frame.mpc.horizonMs)
            .setMpcPredictedPressureKpa(frame.mpc.predictedPressureKpa)
            .build()
}

