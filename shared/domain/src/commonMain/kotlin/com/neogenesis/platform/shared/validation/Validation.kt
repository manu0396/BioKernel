package com.neogenesis.platform.shared.validation

import com.neogenesis.platform.shared.errors.DomainError
import com.neogenesis.platform.shared.errors.DomainResult
import com.neogenesis.platform.shared.telemetry.TelemetryFrame

object TelemetryValidation {
    fun validate(frame: TelemetryFrame): DomainResult<TelemetryFrame> {
        if (frame.pressure.kpa < 0) return DomainResult.Failure(DomainError.ValidationError("Pressure must be >= 0"))
        if (frame.temperature.celsius < -20) return DomainResult.Failure(DomainError.ValidationError("Temperature out of range"))
        if (frame.flowRate.microlitersPerSecond < 0) return DomainResult.Failure(DomainError.ValidationError("Flow rate must be >= 0"))
        return DomainResult.Success(frame)
    }
}
