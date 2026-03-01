package com.neogenesis.platform.shared.telemetry

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

@Serializable
data class PressureReading(val kpa: Double)

@Serializable
data class NozzleDisplacement(val micrometers: Double)

@Serializable
data class FlowRate(val microlitersPerSecond: Double)

@Serializable
data class Temperature(val celsius: Double)

@Serializable
data class ViscosityEstimation(val pascalSecond: Double)

@Serializable
data class PIDState(
    val proportional: Double,
    val integral: Double,
    val derivative: Double
)

@Serializable
data class
MPCPrediction(
    val horizonMs: Int,
    val predictedPressureKpa: Double
)

@Serializable
data class TelemetryFrame(
    val timestamp: Instant,
    val pressure: PressureReading,
    val displacement: NozzleDisplacement,
    val flowRate: FlowRate,
    val temperature: Temperature,
    val viscosity: ViscosityEstimation,
    val pid: PIDState,
    val mpc: MPCPrediction
)

class TelemetryRingBuffer(private val capacity: Int) {
    private val buffer = ArrayList<TelemetryFrame>(capacity)

    @Synchronized
    fun add(frame: TelemetryFrame) {
        if (buffer.size == capacity) {
            buffer.removeAt(0)
        }
        buffer.add(frame)
    }

    @Synchronized
    fun snapshot(): List<TelemetryFrame> = buffer.toList()
}

class TelemetryAggregator {
    fun aggregate(frames: List<TelemetryFrame>): TelemetrySummary {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        val minPressure = frames.minOf { it.pressure.kpa }
        val maxPressure = frames.maxOf { it.pressure.kpa }
        val avgPressure = frames.sumOf { it.pressure.kpa } / frames.size
        val avgFlow = frames.sumOf { it.flowRate.microlitersPerSecond } / frames.size
        val avgTemp = frames.sumOf { it.temperature.celsius } / frames.size
        val avgViscosity = frames.sumOf { it.viscosity.pascalSecond } / frames.size
        return TelemetrySummary(
            minPressureKpa = minPressure,
            maxPressureKpa = maxPressure,
            avgPressureKpa = avgPressure,
            avgFlowRate = avgFlow,
            avgTemperature = avgTemp,
            avgViscosity = avgViscosity
        )
    }
}

@Serializable
data class TelemetrySummary(
    val minPressureKpa: Double,
    val maxPressureKpa: Double,
    val avgPressureKpa: Double,
    val avgFlowRate: Double,
    val avgTemperature: Double,
    val avgViscosity: Double
)

class TelemetryReplay(private val frames: List<TelemetryFrame>) {
    private var index = 0

    fun hasNext(): Boolean = index < frames.size

    fun next(): TelemetryFrame {
        require(hasNext()) { "No more frames" }
        return frames[index++]
    }
}

class TelemetryRateLimiter(private val minIntervalMs: Long) {
    private var lastTimestamp: Long = 0

    fun shouldAccept(timestampMs: Long): Boolean {
        val delta = timestampMs - lastTimestamp
        return if (delta >= minIntervalMs) {
            lastTimestamp = timestampMs
            true
        } else {
            false
        }
    }
}

class TelemetryDownsampler(private val windowMs: Long) {
    fun downsample(frames: List<TelemetryFrame>): List<TelemetryFrame> {
        if (frames.isEmpty()) return emptyList()
        val sorted = frames.sortedBy { it.timestamp }
        val result = mutableListOf<TelemetryFrame>()
        var bucketStart = sorted.first().timestamp
        var bucket = mutableListOf<TelemetryFrame>()
        for (frame in sorted) {
            val delta = frame.timestamp.toEpochMilliseconds() - bucketStart.toEpochMilliseconds()
            if (delta >= windowMs && bucket.isNotEmpty()) {
                result.add(average(bucket))
                bucket = mutableListOf()
                bucketStart = frame.timestamp
            }
            bucket.add(frame)
        }
        if (bucket.isNotEmpty()) result.add(average(bucket))
        return result
    }

    private fun average(frames: List<TelemetryFrame>): TelemetryFrame {
        val avgPressure = frames.sumOf { it.pressure.kpa } / frames.size
        val avgDisplacement = frames.sumOf { it.displacement.micrometers } / frames.size
        val avgFlow = frames.sumOf { it.flowRate.microlitersPerSecond } / frames.size
        val avgTemp = frames.sumOf { it.temperature.celsius } / frames.size
        val avgViscosity = frames.sumOf { it.viscosity.pascalSecond } / frames.size
        val avgPidP = frames.sumOf { it.pid.proportional } / frames.size
        val avgPidI = frames.sumOf { it.pid.integral } / frames.size
        val avgPidD = frames.sumOf { it.pid.derivative } / frames.size
        val avgMpc = frames.sumOf { it.mpc.predictedPressureKpa } / frames.size
        return TelemetryFrame(
            timestamp = frames.last().timestamp,
            pressure = PressureReading(avgPressure),
            displacement = NozzleDisplacement(avgDisplacement),
            flowRate = FlowRate(avgFlow),
            temperature = Temperature(avgTemp),
            viscosity = ViscosityEstimation(avgViscosity),
            pid = PIDState(avgPidP, avgPidI, avgPidD),
            mpc = MPCPrediction(frames.last().mpc.horizonMs, avgMpc)
        )
    }
}

object TelemetryExport {
    private val json = Json { encodeDefaults = true }

    fun toJson(frames: List<TelemetryFrame>): String = json.encodeToString(frames)

    fun toCsv(frames: List<TelemetryFrame>): String {
        val header = "timestamp,pressure_kpa,displacement_um,flow_rate_ul_s,temperature_c,viscosity_pas,pid_p,pid_i,pid_d,mpc_horizon_ms,mpc_predicted_pressure_kpa"
        val rows = frames.joinToString("\n") {
            listOf(
                it.timestamp.toEpochMilliseconds(),
                it.pressure.kpa,
                it.displacement.micrometers,
                it.flowRate.microlitersPerSecond,
                it.temperature.celsius,
                it.viscosity.pascalSecond,
                it.pid.proportional,
                it.pid.integral,
                it.pid.derivative,
                it.mpc.horizonMs,
                it.mpc.predictedPressureKpa
            ).joinToString(",")
        }
        return header + "\n" + rows
    }
}

fun clampPressure(value: Double, minKpa: Double, maxKpa: Double): Double {
    return max(minKpa, min(maxKpa, value))
}
