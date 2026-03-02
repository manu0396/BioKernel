package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.core.device.DevicePolicyRepository
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

object MetricsModule {
    @Serializable
    data class ReproducibilityScoreResponse(val score: Int)

    @Serializable
    data class DriftAlertResponse(
        val id: String,
        val title: String,
        val severity: String,
        val message: String,
        val createdAt: String? = null
    )

    @Serializable
    data class DriftAlertsResponse(val alerts: List<DriftAlertResponse>)

    fun register(
        app: Application,
        policyRepository: DevicePolicyRepository,
        auditLogger: AuditLogger
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/metrics") {
                    get("/reproducibility-score") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "AUDITOR", "RESEARCHER"))) return@get
                        if (!call.requireCapability(Capability.QC_REVIEW, policyRepository, auditLogger)) return@get
                        val runCount = RegenOpsInMemoryStore.runCount()
                        val score = (88 + (runCount % 10)).coerceIn(60, 99)
                        call.respond(ReproducibilityScoreResponse(score))
                    }
                    get("/drift-alerts") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "AUDITOR", "RESEARCHER"))) return@get
                        if (!call.requireCapability(Capability.QC_REVIEW, policyRepository, auditLogger)) return@get
                        val alerts = listOf(
                            DriftAlertResponse(
                                id = "drift-001",
                                title = "Pressure variance",
                                severity = "MEDIUM",
                                message = "Simulated run shows elevated pressure variance in phase 2."
                            ),
                            DriftAlertResponse(
                                id = "drift-002",
                                title = "Thermal slope",
                                severity = "LOW",
                                message = "Thermal slope trending above baseline in batch window."
                            )
                        )
                        call.respond(DriftAlertsResponse(alerts))
                    }
                }
            }
        }
    }
}
