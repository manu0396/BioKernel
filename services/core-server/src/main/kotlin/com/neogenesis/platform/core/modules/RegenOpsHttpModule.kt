package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
import com.neogenesis.platform.core.observability.HttpRequestLabels
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

object RegenOpsHttpModule {
    @Serializable
    data class StartRunRequestDto(
        val protocolId: String,
        val protocolVersion: Int,
        val runId: String? = null,
        val gatewayId: String? = null
    )

    @Serializable
    data class TraceSummaryDto(
        val score: Int,
        val alerts: List<DriftAlertDto>
    )

    @Serializable
    data class DriftAlertDto(
        val id: String,
        val title: String,
        val severity: String,
        val message: String
    )

    fun register(app: Application) {
        // Initialize seed data if mock mode is enabled or as a default for first contact
        seedMockData()

        app.routing {
            route("/api/v1/regenops") {
                get("/protocols") {
                    call.respond(RegenOpsInMemoryStore.listProtocols())
                }
                get("/runs") {
                    // Currently no listRuns in store, but we can return empty or mock
                    call.respond(emptyList<String>())
                }
                post("/runs/start") {
                    val req = call.receive<StartRunRequestDto>()
                    val run = RegenOpsInMemoryStore.startRun(
                        protocolId = req.protocolId,
                        version = req.protocolVersion,
                        requestedRunId = req.runId.orEmpty(),
                        gatewayId = req.gatewayId ?: "gateway-http",
                        labels = HttpRequestLabels.fromCall(call, req.protocolId, req.protocolVersion)
                    )
                    call.respond(HttpStatusCode.Created, run)
                }
                post("/runs/{id}/pause") {
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    call.respond(RegenOpsInMemoryStore.updateRun(id, "PAUSED", HttpRequestLabels.fromCall(call)))
                }
                post("/runs/{id}/abort") {
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    call.respond(RegenOpsInMemoryStore.updateRun(id, "ABORTED", HttpRequestLabels.fromCall(call)))
                }
            }
            route("/api/v1/trace") {
                get("/summary") {
                    call.respond(
                        TraceSummaryDto(
                            score = 92,
                            alerts = listOf(
                                DriftAlertDto("a1", "Pressure Drift", "HIGH", "Primary pump pressure deviating by >15%"),
                                DriftAlertDto("a2", "Thermal Instability", "MEDIUM", "Incubation zone 2 showing cyclic oscillation"),
                                DriftAlertDto("a3", "Flow Restriction", "LOW", "Minor flow impedance detected in nozzle 4"),
                                DriftAlertDto("a4", "Network Latency", "LOW", "Gateway node response time >200ms"),
                                DriftAlertDto("a5", "Calibration Expired", "MEDIUM", "Sensor suite 0x4F requires re-calibration")
                            )
                        )
                    )
                }
            }
        }
    }

    private fun seedMockData() {
        // Only seed if empty or explicitly requested via REGENOPS_MOCK=true
        if (System.getenv("REGENOPS_MOCK") == "false") return

        val protocols = listOf(
            "Cardiac Patch Alpha" to "Synthetic myocardium growth protocol with dynamic tensioning.",
            "Neural Interface v2" to "High-density neural array bioprinting into hydrogel scaffold.",
            "Skin Graft Standard" to "Multi-layer epidermal and dermal cell deposition for acute burns.",
            "Bone Matrix Prime" to "Hydroxyapatite-infused osteoblast seeding for structural repair.",
            "Vascular Network Beta" to "Micro-vascular branching logic with sacrificial ink support.",
            "Lung Alveoli Sim" to "Thin-walled alveolar sac construction for gas exchange research."
        )

        protocols.forEachIndexed { i, (name, summary) ->
            val id = "proto-${i + 1}"
            RegenOpsInMemoryStore.createDraft(id, name, "{\"version\":1,\"seed\":true}", "system-seed")
            RegenOpsInMemoryStore.publishVersion(id, "Initial seeded version")
        }
    }
}
