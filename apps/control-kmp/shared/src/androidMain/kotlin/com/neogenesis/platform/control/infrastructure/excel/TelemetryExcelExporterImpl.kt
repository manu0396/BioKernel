package com.neogenesis.platform.control.infrastructure.excel

import com.neogenesis.platform.control.domain.usecase.TelemetryExcelExporter
import com.neogenesis.platform.shared.telemetry.TelemetryFrame

class TelemetryExcelExporterImpl : TelemetryExcelExporter {
    override fun toXlsxBytes(
        frames: List<TelemetryFrame>,
        runId: String,
        createdAtIso: String,
    ): ByteArray {
        throw UnsupportedOperationException(
            "XLSX export is supported on Desktop (JVM) only in v1.0.0. Use CSV export on Android or export via backend."
        )
    }
}