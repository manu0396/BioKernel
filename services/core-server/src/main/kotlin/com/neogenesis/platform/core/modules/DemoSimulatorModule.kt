package com.neogenesis.platform.core.modules

import com.neogenesis.grpc.GatewayRunEvent
import com.neogenesis.grpc.GatewayTelemetry
import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
import com.neogenesis.platform.core.observability.HttpRequestLabels
import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.shared.domain.device.Capability
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.sin

object DemoSimulatorModule {
    @Serializable
    data class SimulatorRunRequest(
        val protocolId: String,
        val runId: String? = null,
        val samples: Int,
        val intervalMs: Int,
        val failureAt: Int? = null
    )

    @Serializable
    data class SimulatorRunResponse(
        val runId: String
    )

    fun register(
        app: Application,
        auditLogger: AuditLogger,
        policyRepository: DevicePolicyRepository
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/demo/simulator") {
                    post("/runs") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        if (!call.requireCapability(Capability.PRINT_CONTROL, policyRepository, auditLogger)) return@post
                        val tenantId = call.request.queryParameters["tenant_id"]
                        if (tenantId.isNullOrBlank()) {
                            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_tenant_id"))
                        }
                        val correlationId = call.request.headers["X-Correlation-Id"]
                        if (correlationId.isNullOrBlank()) {
                            return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing_correlation_id"))
                        }

                        val req = call.receive<SimulatorRunRequest>()
                        val samples = req.samples.coerceIn(5, 240)
                        val intervalMs = req.intervalMs.coerceIn(50, 1000)
                        val version = 1
                        val labels = HttpRequestLabels.fromCall(call, req.protocolId, version)
                        val run = RegenOpsInMemoryStore.startRun(
                            protocolId = req.protocolId,
                            version = version,
                            requestedRunId = req.runId.orEmpty(),
                            gatewayId = "sim-gateway",
                            labels = labels
                        )

                        call.respond(HttpStatusCode.Created, SimulatorRunResponse(run.runId))

                        app.launch {
                            delay(250)
                            RegenOpsInMemoryStore.pushGatewayEvents(
                                listOf(
                                    GatewayRunEvent.newBuilder()
                                        .setRunId(run.runId)
                                        .setEventType("SIMULATION_STARTED")
                                        .setPayloadJson("{\"tenantId\":\"${tenantId}\"}")
                                        .setCreatedAtMs(System.currentTimeMillis())
                                        .setSeq(1)
                                        .build()
                                )
                            )

                            var seq = 2L
                            val failureAt = req.failureAt?.coerceIn(1, samples)
                            repeat(samples) { idx ->
                                delay(intervalMs.toLong())
                                val step = idx + 1
                                val driftBoost = if (failureAt != null && step >= failureAt) 0.65 else 0.05
                                val base = 105.0 + (sin(step / 3.0) * 6.0)
                                val value = base + (driftBoost * 20.0)
                                val telemetry = GatewayTelemetry.newBuilder()
                                    .setRunId(run.runId)
                                    .setMetricKey("pressure")
                                    .setMetricValue(value)
                                    .setUnit("kPa")
                                    .setDriftScore(driftBoost)
                                    .setRecordedAtMs(System.currentTimeMillis())
                                    .setSeq(seq++)
                                    .build()
                                RegenOpsInMemoryStore.pushGatewayTelemetry("sim-gateway", listOf(telemetry))

                                if (failureAt != null && step == failureAt) {
                                    RegenOpsInMemoryStore.pushGatewayEvents(
                                        listOf(
                                            GatewayRunEvent.newBuilder()
                                                .setRunId(run.runId)
                                                .setEventType("DRIFT_ALERT")
                                                .setPayloadJson("{\"severity\":\"MEDIUM\",\"message\":\"Drift threshold reached\"}")
                                                .setCreatedAtMs(System.currentTimeMillis())
                                                .setSeq(seq++)
                                                .build()
                                        )
                                    )
                                }
                            }

                            RegenOpsInMemoryStore.updateRun(run.runId, "COMPLETED", labels)
                            RegenOpsInMemoryStore.pushGatewayEvents(
                                listOf(
                                    GatewayRunEvent.newBuilder()
                                        .setRunId(run.runId)
                                        .setEventType("SIMULATION_COMPLETED")
                                        .setPayloadJson("{\"samples\":${samples}}")
                                        .setCreatedAtMs(System.currentTimeMillis())
                                        .setSeq(seq++)
                                        .build()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
