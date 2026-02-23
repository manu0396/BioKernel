package com.neogenesis.platform.backend.grpc

import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class TelemetryBus {
    private val flow = MutableSharedFlow<TelemetryFrame>(replay = 1, extraBufferCapacity = 10_000)

    suspend fun emit(frame: TelemetryFrame) {
        flow.emit(frame)
    }

    fun stream(): SharedFlow<TelemetryFrame> = flow
}
