package com.neogenesis.platform.control.data.stream

import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.coroutines.flow.Flow

interface RegenOpsStreamClient {
    fun streamEvents(runId: String): Flow<RunEvent>
    fun streamTelemetry(runId: String): Flow<TelemetryFrame>
    fun close()
}
