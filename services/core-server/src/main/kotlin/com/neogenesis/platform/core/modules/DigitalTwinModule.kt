package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.shared.digitaltwin.DigitalTwinEngine
import com.neogenesis.platform.shared.digitaltwin.DigitalTwinParameters
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.core.device.DevicePolicyRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*

object DigitalTwinModule {
    fun register(
        app: Application,
        policyRepository: DevicePolicyRepository,
        auditLogger: AuditLogger
    ) {
        val engine = DigitalTwinEngine(
            DigitalTwinParameters(
                nozzleRadiusMicrometers = 200.0,
                maxPressureKpa = 300.0,
                minPressureKpa = 0.0,
                viscosityCompensation = 0.5,
                flowGain = 2.0
            )
        )
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/digital-twin") {
                    post("/simulate") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER"))) return@post
                        if (!call.requireCapability(Capability.LIVE_MONITOR, policyRepository, auditLogger)) return@post
                        val frame = call.receive<TelemetryFrame>()
                        val result = engine.simulate(frame)
                        call.respond(HttpStatusCode.OK, result)
                    }
                    get("/status") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER"))) return@get
                        if (!call.requireCapability(Capability.LIVE_MONITOR, policyRepository, auditLogger)) return@get
                        call.respond(mapOf("status" to "ok"))
                    }
                }
            }
        }
    }
}
