package com.neogenesis.platform.control.domain.usecase

import com.neogenesis.platform.shared.telemetry.TelemetryFrame

interface TelemetryExcelExporter {
    fun toXlsxBytes(
        frames: List<TelemetryFrame>,
        runId: String,
        createdAtIso: String,
    ): ByteArray
}
