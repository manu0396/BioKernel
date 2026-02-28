package com.neogenesis.platform.control.presentation

enum class SimulationState {
    Idle,
    Running,
    Paused,
    STOPPED, // Manually stopped
    FINISHED, // Naturally completed
    FAILED, // Encountered an error
    Exporting // Currently exporting telemetry
}
