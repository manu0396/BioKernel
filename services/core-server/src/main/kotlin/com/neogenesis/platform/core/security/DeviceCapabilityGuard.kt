package com.neogenesis.platform.core.security

import com.neogenesis.platform.core.device.DeviceContext
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.shared.domain.device.Capability
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

private val deviceGuardLogger = LoggerFactory.getLogger("DeviceCapabilityGuard")

suspend fun ApplicationCall.requireCapability(
    required: Capability,
    policyRepository: DevicePolicyRepository
): Boolean {
    val policy = policyRepository.load()
    val ctx = DeviceContext.fromCall(this, policy)
    if (!ctx.effectiveCapabilities.contains(required)) {
        val subject = jwtSubject() ?: "unknown"
        deviceGuardLogger.warn(
            "device_capability_denied subject={} deviceClass={} tier={} path={} method={} required={}",
            subject,
            ctx.deviceInfo.deviceClass,
            ctx.deviceInfo.tier,
            request.path(),
            request.httpMethod.value,
            required.name
        )
        respond(HttpStatusCode.Forbidden, mapOf("error" to "device_capability_denied", "required" to required.name))
        return false
    }
    return true
}
