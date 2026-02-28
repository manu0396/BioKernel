package com.neogenesis.platform.control.presentation

/**
 * Simulation parameters selected by the operator before starting a simulated run.
 * Wire into backend when API supports it.
 */
data class SimulationConfig(
    val durationMinutes: Int = 30,
    val tickMillis: Int = 250,
    val speedFactor: Double = 1.0,
    val operatorName: String = "",
    val notes: String = "",
)

