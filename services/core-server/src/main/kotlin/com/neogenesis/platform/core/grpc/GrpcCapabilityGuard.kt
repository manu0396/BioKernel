package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.observability.BusinessMetrics
import com.neogenesis.platform.core.storage.SystemIds
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.shared.domain.device.Capability
import io.grpc.Status
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

object GrpcCapabilityGuard {
    private val logger = LoggerFactory.getLogger("GrpcCapabilityGuard")
    @Volatile
    var auditLogger: AuditLogger? = null

    fun requireCapability(required: Capability) {
        val ctx = io.grpc.Context.current()
        val caps = GrpcDeviceContext.capsKey.get()
        if (caps == null || !caps.contains(required)) {
            val info = GrpcDeviceContext.deviceInfoKey.get()
            val method = GrpcDeviceContext.methodKey.get() ?: "unknown"
            if (auditLogger != null) {
                runBlocking {
                    auditLogger?.appendEvent(
                        jobId = PrintJobId(SystemIds.jobId.toString()),
                        actorId = UserId(SystemIds.userId.toString()),
                        deviceId = DeviceId(info?.deviceId ?: SystemIds.deviceId.toString()),
                        eventType = "DEVICE_CAPABILITY_DENIED",
                        payload = buildString {
                            append("{\"capability\":\"")
                            append(required.name)
                            append("\",\"deviceClass\":\"")
                            append(info?.deviceClass?.name ?: "UNKNOWN")
                            append("\",\"tier\":\"")
                            append(info?.tier?.name ?: "UNKNOWN")
                            append("\",\"method\":\"")
                            append(method)
                            append("\"}")
                        }
                    )
                }
            }
            BusinessMetrics.deviceCapabilityDecision(required.name, "denied")
            logger.warn(
                "grpc_device_capability_denied deviceClass={} tier={} method={} required={}",
                info?.deviceClass,
                info?.tier,
                method,
                required.name
            )
            throw Status.PERMISSION_DENIED
                .withDescription("device_capability_denied: ${required.name}")
                .asRuntimeException()
        }
        val info = GrpcDeviceContext.deviceInfoKey.get()
        val method = GrpcDeviceContext.methodKey.get() ?: "unknown"
        if (auditLogger != null) {
            runBlocking {
                auditLogger?.appendEvent(
                    jobId = PrintJobId(SystemIds.jobId.toString()),
                    actorId = UserId(SystemIds.userId.toString()),
                    deviceId = DeviceId(info?.deviceId ?: SystemIds.deviceId.toString()),
                    eventType = "DEVICE_CAPABILITY_ALLOWED",
                    payload = buildString {
                        append("{\"capability\":\"")
                        append(required.name)
                        append("\",\"deviceClass\":\"")
                        append(info?.deviceClass?.name ?: "UNKNOWN")
                        append("\",\"tier\":\"")
                        append(info?.tier?.name ?: "UNKNOWN")
                        append("\",\"method\":\"")
                        append(method)
                        append("\"}")
                    }
                )
            }
        }
        BusinessMetrics.deviceCapabilityDecision(required.name, "allowed")
    }
}
