package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.shared.domain.device.Capability
import io.grpc.Status
import org.slf4j.LoggerFactory

object GrpcCapabilityGuard {
    private val logger = LoggerFactory.getLogger("GrpcCapabilityGuard")

    fun requireCapability(required: Capability) {
        val caps = GrpcDeviceContext.capsKey.get()
        if (caps == null || !caps.contains(required)) {
            val info = GrpcDeviceContext.deviceInfoKey.get()
            logger.warn(
                "grpc_device_capability_denied deviceClass={} tier={} method={} required={}",
                info?.deviceClass,
                info?.tier,
                io.grpc.Context.current().get(io.grpc.Context.key<String>("grpc-method")) ?: "unknown",
                required.name
            )
            throw Status.PERMISSION_DENIED
                .withDescription("device_capability_denied: ${required.name}")
                .asRuntimeException()
        }
    }
}
