package com.neogenesis.platform.shared.digitaltwin

import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Serializable
data class DigitalTwinParameters(
    val nozzleRadiusMicrometers: Double,
    val maxPressureKpa: Double,
    val minPressureKpa: Double,
    val viscosityCompensation: Double,
    val flowGain: Double
)

@Serializable
data class DeviationMetrics(
    val pressureDeviationKpa: Double,
    val flowDeviation: Double,
    val viscosityDeviation: Double,
    val normalizedScore: Double
)

class DigitalTwinEngine(private val params: DigitalTwinParameters) {
    fun simulate(frame: TelemetryFrame): DigitalTwinResult {
        val expectedFlow = params.flowGain * frame.pressure.kpa / max(0.1, frame.viscosity.pascalSecond)
        val expectedPressure = frame.flowRate.microlitersPerSecond * frame.viscosity.pascalSecond / max(0.1, params.flowGain)

        val adjustedPressure = clamp(frame.pressure.kpa + viscosityCorrection(frame))
        val pressureDeviation = adjustedPressure - expectedPressure
        val flowDeviation = frame.flowRate.microlitersPerSecond - expectedFlow
        val viscosityDeviation = frame.viscosity.pascalSecond - estimatedViscosity(frame)

        val score = normalizeScore(pressureDeviation, flowDeviation, viscosityDeviation)

        val metrics = DeviationMetrics(
            pressureDeviationKpa = pressureDeviation,
            flowDeviation = flowDeviation,
            viscosityDeviation = viscosityDeviation,
            normalizedScore = score
        )

        return DigitalTwinResult(expectedPressure, expectedFlow, metrics)
    }

    private fun viscosityCorrection(frame: TelemetryFrame): Double {
        return params.viscosityCompensation * (frame.viscosity.pascalSecond - 1.0)
    }

    private fun estimatedViscosity(frame: TelemetryFrame): Double {
        return max(0.1, frame.temperature.celsius / 100.0) * params.nozzleRadiusMicrometers / 1000.0
    }

    private fun normalizeScore(p: Double, f: Double, v: Double): Double {
        val normalized = abs(p) + abs(f) + abs(v)
        return min(1.0, normalized / 100.0)
    }

    private fun clamp(value: Double): Double =
        min(params.maxPressureKpa, max(params.minPressureKpa, value))
}

@Serializable
data class DigitalTwinResult(
    val expectedPressureKpa: Double,
    val expectedFlowRate: Double,
    val deviation: DeviationMetrics
)
