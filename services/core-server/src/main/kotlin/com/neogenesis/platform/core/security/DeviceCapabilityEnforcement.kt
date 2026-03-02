package com.neogenesis.platform.core.security

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.device.DeviceContext
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.storage.SystemIds
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.UserId
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond

class DeviceCapabilityEnforcementConfig {
    val allowlistPaths: MutableList<Regex> = mutableListOf()
    var auditLogger: AuditLogger? = null
    var policyRepository: DevicePolicyRepository? = null
}

val DeviceCapabilityEnforcement = createApplicationPlugin(
    name = "DeviceCapabilityEnforcement",
    createConfiguration = ::DeviceCapabilityEnforcementConfig
) {
    val allowlist = pluginConfig.allowlistPaths.toList()
    val auditLogger = pluginConfig.auditLogger
    val policyRepository = pluginConfig.policyRepository
    val mutatingMethods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Delete, HttpMethod.Patch)
    val mappedMutations: List<Pair<HttpMethod, Regex>> = listOf(
        HttpMethod.Post to Regex("^/print-sessions$"),
        HttpMethod.Post to Regex("^/print-sessions/[^/]+/(activate|complete|abort)$"),
        HttpMethod.Post to Regex("^/api/v1/runs/start$"),
        HttpMethod.Post to Regex("^/api/v1/runs/[^/]+/control$"),
        HttpMethod.Post to Regex("^/api/v1/regenops/runs/start$"),
        HttpMethod.Post to Regex("^/api/v1/regenops/runs/[^/]+/(pause|abort)$"),
        HttpMethod.Post to Regex("^/runs/start$"),
        HttpMethod.Post to Regex("^/runs/[^/]+/(pause|abort)$"),
        HttpMethod.Post to Regex("^/api/v1/regenops/protocols$"),
        HttpMethod.Post to Regex("^/api/v1/regenops/protocols/[^/]+/publish$"),
        HttpMethod.Post to Regex("^/api/v1/protocols/[^/]+/versions/[^/]+/publish$"),
        HttpMethod.Post to Regex("^/protocols/[^/]+/publish$"),
        HttpMethod.Post to Regex("^/api/v1/protocols/.*$"),
        HttpMethod.Put to Regex("^/api/v1/protocols/.*$"),
        HttpMethod.Delete to Regex("^/api/v1/protocols/.*$"),
        HttpMethod.Post to Regex("^/api/v1/recipes$"),
        HttpMethod.Post to Regex("^/api/v1/recipes/[^/]+/activate$"),
        HttpMethod.Put to Regex("^/api/v1/recipes/[^/]+$"),
        HttpMethod.Delete to Regex("^/api/v1/recipes/[^/]+$"),
        HttpMethod.Post to Regex("^/admin/compliance/protocols/[^/]+/publish-approvals$"),
        HttpMethod.Post to Regex("^/admin/compliance/publish-approvals/[^/]+/approve$"),
        HttpMethod.Post to Regex("^/admin/.*$"),
        HttpMethod.Put to Regex("^/admin/.*$"),
        HttpMethod.Delete to Regex("^/admin/.*$"),
        HttpMethod.Post to Regex("^/api/v1/devices$"),
        HttpMethod.Post to Regex("^/api/v1/devices/[^/]+/pair$"),
        HttpMethod.Post to Regex("^/api/v1/devices/[^/]+/pair/complete$"),
        HttpMethod.Post to Regex("^/api/v1/bioink/profiles$"),
        HttpMethod.Post to Regex("^/api/v1/bioink/batches$"),
        HttpMethod.Post to Regex("^/api/v1/print-jobs$"),
        HttpMethod.Post to Regex("^/api/v1/telemetry/.*$"),
        HttpMethod.Post to Regex("^/api/v1/device/register$"),
        HttpMethod.Post to Regex("^/demo/simulator/runs$")
    )

    onCall { call ->
        val method = call.request.httpMethod
        if (!mutatingMethods.contains(method)) return@onCall
        if (allowlist.any { it.matches(call.request.path()) }) return@onCall
        val path = call.request.path()
        val mapped = mappedMutations.any { (mappedMethod, regex) ->
            mappedMethod == method && regex.matches(path)
        }
        if (mapped) return@onCall
        val ctx = policyRepository?.load()?.let { DeviceContext.fromCall(call, it) }
        val subject = call.jwtSubject()
        val actorId = runCatching { subject?.let { UserId(it) } }.getOrNull()
            ?: UserId(SystemIds.userId.toString())
        auditLogger?.appendEvent(
            jobId = PrintJobId(SystemIds.jobId.toString()),
            actorId = actorId,
            deviceId = DeviceId(ctx?.deviceInfo?.deviceId ?: SystemIds.deviceId.toString()),
            eventType = "DEVICE_CAPABILITY_UNMAPPED",
            payload = buildString {
                append("{\"path\":\"")
                append(path)
                append("\",\"method\":\"")
                append(method.value)
                append("\"}")
            }
        )
        call.respond(
            HttpStatusCode.Forbidden,
            mapOf("error" to "device_capability_unmapped", "path" to path)
        )
        return@onCall
    }

    onCallRespond { call ->
        val method = call.request.httpMethod
        if (!mutatingMethods.contains(method)) return@onCallRespond
        val capability = call.requiredCapabilityOrNull() ?: return@onCallRespond
        val status = call.response.status() ?: HttpStatusCode.OK
        if (status.value >= 400) return@onCallRespond
        val ctx = policyRepository?.load()?.let { DeviceContext.fromCall(call, it) }
        val subject = call.jwtSubject()
        val actorId = runCatching { subject?.let { UserId(it) } }.getOrNull()
            ?: UserId(SystemIds.userId.toString())
        auditLogger?.appendEvent(
            jobId = PrintJobId(SystemIds.jobId.toString()),
            actorId = actorId,
            deviceId = DeviceId(ctx?.deviceInfo?.deviceId ?: SystemIds.deviceId.toString()),
            eventType = "DEVICE_CAPABILITY_ALLOWED",
            payload = buildString {
                append("{\"capability\":\"")
                append(capability.name)
                append("\",\"path\":\"")
                append(call.request.path())
                append("\",\"method\":\"")
                append(method.value)
                append("\"}")
            }
        )
    }
}
