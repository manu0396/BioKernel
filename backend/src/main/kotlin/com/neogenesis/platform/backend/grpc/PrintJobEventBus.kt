package com.neogenesis.platform.backend.grpc

import com.neogenesis.platform.proto.v1.PrintJobEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PrintJobEventBus {
    private val flow = MutableSharedFlow<PrintJobEvent>(extraBufferCapacity = 1_000)

    suspend fun emit(event: PrintJobEvent) {
        flow.emit(event)
    }

    fun stream(): SharedFlow<PrintJobEvent> = flow
}
