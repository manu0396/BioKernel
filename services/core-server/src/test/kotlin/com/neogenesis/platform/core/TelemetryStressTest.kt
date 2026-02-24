package com.neogenesis.platform.core

import com.neogenesis.platform.shared.telemetry.*
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryStressTest {
    @Test
    fun bufferMaintainsCapacity() {
        val buffer = TelemetryRingBuffer(100)
        repeat(1000) {
            val frame = TelemetryFrame(
                timestamp = Clock.System.now(),
                pressure = PressureReading(100.0 + it),
                displacement = NozzleDisplacement(1.0),
                flowRate = FlowRate(1.0),
                temperature = Temperature(25.0),
                viscosity = ViscosityEstimation(1.0),
                pid = PIDState(0.1, 0.2, 0.3),
                mpc = MPCPrediction(20, 110.0)
            )
            buffer.add(frame)
        }
        assertEquals(100, buffer.snapshot().size)
    }
}

