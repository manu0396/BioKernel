package com.neogenesis.platform.core.security

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.device.DeviceContext
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.observability.BusinessMetrics
import com.neogenesis.platform.core.storage.SystemIds
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

private val deviceGuardLogger = LoggerFactory.getLogger("DeviceCapabilityGuard")
private val capabilityCheckedKey = AttributeKey<Boolean>("device_capability_checked")
private val requiredCapabilityKey = AttributeKey<Capability>("device_required_capability")

suspend fun ApplicationCall.requireCapability(
    required: Capability,
    policyRepository: DevicePolicyRepository,
    auditLogger: AuditLogger? = null
): Boolean {
    attributes.put(capabilityCheckedKey, true)
    attributes.put(requiredCapabilityKey, required)
    val policy = policyRepository.load()
    val ctx = DeviceContext.fromCall(this, policy)
    val subject = jwtSubject()
    val actorId = runCatching { subject?.let { UserId(it) } }.getOrNull()
        ?: UserId(SystemIds.userId.toString())
    if (!ctx.effectiveCapabilities.contains(required)) {
        deviceGuardLogger.warn(
            "device_capability_denied subject={} deviceClass={} tier={} path={} method={} required={}",
            subject ?: "unknown",
            ctx.deviceInfo.deviceClass,
            ctx.deviceInfo.tier,
            request.path(),
            request.httpMethod.value,
            required.name
        )
        auditLogger?.appendEvent(
            jobId = PrintJobId(SystemIds.jobId.toString()),
            actorId = actorId,
            deviceId = DeviceId(ctx.deviceInfo.deviceId ?: SystemIds.deviceId.toString()),
            eventType = "DEVICE_CAPABILITY_DENIED",
            payload = buildString {
                append("{\"capability\":\"")
                append(required.name)
                append("\",\"deviceClass\":\"")
                append(ctx.deviceInfo.deviceClass.name)
                append("\",\"tier\":\"")
                append(ctx.deviceInfo.tier.name)
                append("\",\"path\":\"")
                append(request.path())
                append("\",\"method\":\"")
                append(request.httpMethod.value)
                append("\"}")
            }
        )
        BusinessMetrics.deviceCapabilityDecision(required.name, "denied")
        respond(HttpStatusCode.Forbidden, mapOf("error" to "device_capability_denied", "required" to required.name))
        return false
    }
    BusinessMetrics.deviceCapabilityDecision(required.name, "allowed")
    return true
}

fun ApplicationCall.markCapabilityChecked() {
    attributes.put(capabilityCheckedKey, true)
}

fun ApplicationCall.hasCapabilityChecked(): Boolean =
    attributes.getOrNull(capabilityCheckedKey) == true

fun ApplicationCall.requiredCapabilityOrNull(): Capability? =
    attributes.getOrNull(requiredCapabilityKey)
