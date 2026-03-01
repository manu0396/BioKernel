package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.security.enforceRole
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

object CommercialModule {
    @Serializable
    data class CommercialPipelineResponse(
        val stages: Map<String, List<CommercialOpportunityResponse>>
    )

    @Serializable
    data class CommercialOpportunityResponse(
        val id: String,
        val name: String,
        val stage: String,
        val expectedRevenueEur: Double,
        val probability: Int,
        val notes: String,
        val loiSigned: Boolean
    )

    fun register(app: Application) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/commercial") {
                    get("/pipeline") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "AUDITOR"))) return@get
                        val pipeline = demoPipeline()
                        call.respond(CommercialPipelineResponse(pipeline))
                    }
                    get("/pipeline/export") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "AUDITOR"))) return@get
                        val pipeline = demoPipeline()
                        val csv = buildCsv(pipeline)
                        call.response.header("Content-Disposition", "attachment; filename=\"commercial_pipeline.csv\"")
                        call.respondBytes(csv.encodeToByteArray(), contentType = ContentType.Text.CSV)
                    }
                }
            }
        }
    }

    private fun demoPipeline(): Map<String, List<CommercialOpportunityResponse>> {
        val discovery = listOf(
            CommercialOpportunityResponse(
                id = "opp-1001",
                name = "Nova BioFab Pilot",
                stage = "Discovery",
                expectedRevenueEur = 120_000.0,
                probability = 35,
                notes = "Pilot scope for regenerative scaffold trials.",
                loiSigned = false
            )
        )
        val pilot = listOf(
            CommercialOpportunityResponse(
                id = "opp-1002",
                name = "Helix Research Expansion",
                stage = "Pilot",
                expectedRevenueEur = 310_000.0,
                probability = 55,
                notes = "Expand protocol validation to multi-site runbooks.",
                loiSigned = true
            )
        )
        val loi = listOf(
            CommercialOpportunityResponse(
                id = "opp-1003",
                name = "Atlas Medical LOI",
                stage = "LOI",
                expectedRevenueEur = 640_000.0,
                probability = 68,
                notes = "Awaiting regulatory alignment for clinical deployment.",
                loiSigned = true
            )
        )
        val contract = listOf(
            CommercialOpportunityResponse(
                id = "opp-1004",
                name = "Eden Biologics Contract",
                stage = "Contract",
                expectedRevenueEur = 1_250_000.0,
                probability = 82,
                notes = "Final contract review pending procurement sign-off.",
                loiSigned = true
            )
        )
        return linkedMapOf(
            "Discovery" to discovery,
            "Pilot" to pilot,
            "LOI" to loi,
            "Contract" to contract
        )
    }

    private fun buildCsv(pipeline: Map<String, List<CommercialOpportunityResponse>>): String {
        val header = "id,name,stage,expectedRevenueEur,probability,loiSigned,notes"
        val rows = pipeline.values.flatten().joinToString("\n") { opp ->
            "${opp.id},${opp.name},${opp.stage},${opp.expectedRevenueEur},${opp.probability},${opp.loiSigned},${opp.notes.replace(",", ";")}"
        }
        return header + "\n" + rows
    }
}
