package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.shared.domain.device.Capability
import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

object AdminOpsModule {
    fun register(
        app: Application,
        policyRepository: DevicePolicyRepository,
        auditLogger: AuditLogger
    ) {
        app.routing {
            get("/admin/metrics") {
                if (!call.requireCapability(Capability.ADMIN_SETTINGS, policyRepository, auditLogger)) return@get
                call.respond(
                    mapOf(
                        "uptime" to System.currentTimeMillis(),
                        "status" to "ok"
                    )
                )
            }
            get("/openapi.json") {
                val url = app.environment.classLoader.getResource("openapi.json")
                    ?: return@get call.respond(mapOf("error" to "missing openapi"))
                val text = url.openStream().bufferedReader().readText()
                call.respondText(text, io.ktor.http.ContentType.Application.Json)
            }
        }
    }
}
