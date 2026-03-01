package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.evidence.EvidencePackageBuilder
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.storage.TelemetryRepositoryImpl
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.telemetry.TelemetryExport
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

object ExportsAliasModule {
    fun register(
        app: Application,
        telemetryRepository: TelemetryRepositoryImpl,
        evidencePackageBuilder: EvidencePackageBuilder
    ) {
        app.routing {
            authenticate("auth-jwt") {
                get("/evidence-pack/job/{runId}/report.csv") {
                    if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                    val runId = call.parameters["runId"] ?: return@get
                    val frames = telemetryRepository.list(PrintJobId(runId), 10_000)
                    val csv = TelemetryExport.toCsv(frames)
                    call.response.header("Content-Disposition", "attachment; filename=\"evidence_report_${runId}.csv\"")
                    call.respondBytes(csv.encodeToByteArray(), contentType = ContentType.Text.CSV)
                }
                get("/audit-bundle/job/{runId}.zip") {
                    if (!call.enforceRole(setOf("ADMIN", "AUDITOR"))) return@get
                    val runId = call.parameters["runId"] ?: return@get
                    val bundle = evidencePackageBuilder.build(PrintJobId(runId))
                    call.response.header("Content-Disposition", "attachment; filename=\"${bundle.fileName}\"")
                    call.respondBytes(bundle.bytes, contentType = ContentType.parse("application/zip"))
                }
                get("/api/v1/telemetry/{runId}/export") {
                    if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                    val runId = call.parameters["runId"] ?: return@get
                    val frames = telemetryRepository.list(PrintJobId(runId), 10_000)
                    val csv = TelemetryExport.toCsv(frames)
                    call.response.header("Content-Disposition", "attachment; filename=\"run_report_${runId}.csv\"")
                    call.respondBytes(csv.encodeToByteArray(), contentType = ContentType.Text.CSV)
                }
                get("/api/v1/evidence/{runId}/package") {
                    if (!call.enforceRole(setOf("ADMIN", "AUDITOR"))) return@get
                    val runId = call.parameters["runId"] ?: return@get
                    val bundle = evidencePackageBuilder.build(PrintJobId(runId))
                    call.response.header("Content-Disposition", "attachment; filename=\"${bundle.fileName}\"")
                    call.respondBytes(bundle.bytes, contentType = ContentType.parse("application/zip"))
                }
            }
        }
    }
}



