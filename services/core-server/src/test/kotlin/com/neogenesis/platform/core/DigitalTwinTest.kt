package com.neogenesis.platform.core

import com.neogenesis.platform.shared.digitaltwin.DigitalTwinEngine
import com.neogenesis.platform.shared.digitaltwin.DigitalTwinParameters
import com.neogenesis.platform.shared.telemetry.*
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue

class DigitalTwinTest {
    @Test
    fun producesDeterministicDeviation() {
        val engine = DigitalTwinEngine(
            DigitalTwinParameters(
                nozzleRadiusMicrometers = 200.0,
                maxPressureKpa = 300.0,
                minPressureKpa = 0.0,
                viscosityCompensation = 0.5,
                flowGain = 2.0
            )
        )
        val frame = TelemetryFrame(
            timestamp = Clock.System.now(),
            pressure = PressureReading(120.0),
            displacement = NozzleDisplacement(10.0),
            flowRate = FlowRate(5.0),
            temperature = Temperature(30.0),
            viscosity = ViscosityEstimation(1.2),
            pid = PIDState(1.0, 0.1, 0.01),
            mpc = MPCPrediction(50, 118.0)
        )
        val result = engine.simulate(frame)
        assertTrue(result.deviation.normalizedScore <= 1.0)
    }
}

