package com.neogenesis.platform.core.modules

import com.neogenesis.grpc.ProtocolSummary
import com.neogenesis.grpc.ProtocolVersionRecord
import com.neogenesis.grpc.RunRecord
import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
import com.neogenesis.platform.core.observability.HttpRequestLabels
import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.shared.domain.device.Capability
import com.neogenesis.platform.core.device.DevicePolicyRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
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
    data class PublishVersionRequestDto(
        val versionId: String,
        val changelog: String? = null
    )

    @Serializable
    data class ListProtocolsResponseDto(
        val protocols: List<ProtocolSummaryDto> = emptyList()
    )

    @Serializable
    data class ProtocolSummaryDto(
        val protocolId: String,
        val title: String,
        val summary: String? = null,
        val latestVersion: Int
    )

    @Serializable
    data class ProtocolVersionRecordDto(
        val protocolId: String,
        val version: Int,
        val contentJson: String,
        val publishedBy: String? = null
    )

    @Serializable
    data class RunRecordDto(
        val runId: String,
        val protocolId: String,
        val protocolVersion: Int,
        val status: String
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

    fun register(
        app: Application,
        policyRepository: DevicePolicyRepository,
        auditLogger: AuditLogger
    ) {
        seedMockData()

        app.routing {
            route("/api/v1/regenops") {
                get("/protocols") {
                    if (!call.requireCapability(Capability.READ_ONLY_DASHBOARD, policyRepository, auditLogger)) return@get
                    val response = ListProtocolsResponseDto(
                        protocols = RegenOpsInMemoryStore.listProtocols().protocolsList.map { it.toDto() }
                    )
                    call.respond(response)
                }
                get("/runs") {
                    if (!call.requireCapability(Capability.READ_ONLY_DASHBOARD, policyRepository, auditLogger)) return@get
                    call.respond(RegenOpsInMemoryStore.listRuns().map { it.toDto() })
                }
                post("/runs/start") {
                    if (!call.requireCapability(Capability.PRINT_CONTROL, policyRepository, auditLogger)) return@post
                    val req = call.receive<StartRunRequestDto>()
                    val run = RegenOpsInMemoryStore.startRun(
                        protocolId = req.protocolId,
                        version = req.protocolVersion,
                        requestedRunId = req.runId.orEmpty(),
                        gatewayId = req.gatewayId ?: "gateway-http",
                        labels = HttpRequestLabels.fromCall(call, req.protocolId, req.protocolVersion)
                    )
                    call.respond(HttpStatusCode.Created, run.toDto())
                }
                post("/runs/{id}/pause") {
                    if (!call.requireCapability(Capability.PRINT_CONTROL, policyRepository, auditLogger)) return@post
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val run = RegenOpsInMemoryStore.updateRun(id, "PAUSED", HttpRequestLabels.fromCall(call))
                    call.respond(run.toDto())
                }
                post("/runs/{id}/abort") {
                    if (!call.requireCapability(Capability.PRINT_CONTROL, policyRepository, auditLogger)) return@post
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val run = RegenOpsInMemoryStore.updateRun(id, "ABORTED", HttpRequestLabels.fromCall(call))
                    call.respond(run.toDto())
                }
                post("/protocols/{id}/publish") {
                    if (!call.requireCapability(Capability.PROTOCOL_EDIT, policyRepository, auditLogger)) return@post
                    val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val req = call.receive<PublishVersionRequestDto>()
                    val record = RegenOpsInMemoryStore.publishVersion(id, req.changelog ?: "Published via control app")
                    call.respond(record.toDto())
                }
            }
            route("/api/v1/trace") {
                get("/summary") {
                    if (!call.requireCapability(Capability.QC_REVIEW, policyRepository, auditLogger)) return@get
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

    private fun ProtocolSummary.toDto(): ProtocolSummaryDto =
        ProtocolSummaryDto(
            protocolId = protocolId,
            title = title,
            summary = null,
            latestVersion = latestVersion
        )

    private fun ProtocolVersionRecord.toDto(): ProtocolVersionRecordDto =
        ProtocolVersionRecordDto(
            protocolId = protocolId,
            version = version,
            contentJson = contentJson,
            publishedBy = "system"
        )

    private fun RunRecord.toDto(): RunRecordDto =
        RunRecordDto(
            runId = runId,
            protocolId = protocolId,
            protocolVersion = protocolVersion,
            status = status
        )

    private fun seedMockData() {
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
