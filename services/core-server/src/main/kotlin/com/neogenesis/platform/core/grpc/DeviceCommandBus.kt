package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.proto.v1.DeviceControlCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class DeviceCommandBus {
    private val flow = MutableSharedFlow<DeviceControlCommand>(extraBufferCapacity = 1_000)

    suspend fun emit(command: DeviceControlCommand) {
        flow.emit(command)
    }

    fun stream(): SharedFlow<DeviceControlCommand> = flow
}

