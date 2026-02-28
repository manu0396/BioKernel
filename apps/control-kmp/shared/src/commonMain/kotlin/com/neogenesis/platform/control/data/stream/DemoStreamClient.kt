package com.neogenesis.platform.control.data.stream

import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.telemetry.FlowRate
import com.neogenesis.platform.shared.telemetry.MPCPrediction
import com.neogenesis.platform.shared.telemetry.NozzleDisplacement
import com.neogenesis.platform.shared.telemetry.PIDState
import com.neogenesis.platform.shared.telemetry.PressureReading
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import com.neogenesis.platform.shared.telemetry.Temperature
import com.neogenesis.platform.shared.telemetry.ViscosityEstimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.datetime.Clock
import kotlin.math.sin

class DemoStreamClient : RegenOpsStreamClient {
    override fun streamEvents(runId: String): Flow<RunEvent> = channelFlow {
        delay(300)
        send(
            RunEvent(
                id = "evt-1",
                runId = RunId(runId),
                eventType = "RUN_STARTED",
                message = "Run started",
                createdAt = Clock.System.now()
            )
        )
    }

    override fun streamTelemetry(runId: String): Flow<TelemetryFrame> = channelFlow {
        var tick = 0
        while (true) {
            val now = Clock.System.now()
            val pressure = 110.0 + sin(tick / 6.0) * 8.0
            send(
                TelemetryFrame(
                    timestamp = now,
                    pressure = PressureReading(pressure),
                    displacement = NozzleDisplacement(12.0 + sin(tick / 4.0) * 2.0),
                    flowRate = FlowRate(4.0 + sin(tick / 5.0) * 0.4),
                    temperature = Temperature(25.0 + sin(tick / 7.0) * 0.8),
                    viscosity = ViscosityEstimation(1.2 + sin(tick / 3.0) * 0.1),
                    pid = PIDState(0.8, 0.2, 0.05),
                    mpc = MPCPrediction(250, pressure + 1.4)
                )
            )
            tick += 1
            delay(500)
        }
    }

    override fun close() = Unit
}
