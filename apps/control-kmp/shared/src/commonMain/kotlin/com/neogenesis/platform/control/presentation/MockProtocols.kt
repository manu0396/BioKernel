package com.neogenesis.platform.control.presentation

import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * UI fallback protocols so the app is usable even when backend returns empty.
 * Remove when production backend always returns data.
 */
object MockProtocols {
    fun sample(): List<Protocol> {
        val now = Clock.System.now()

        fun version(
            protocolId: String,
            versionId: String,
            versionNumber: Int,
            published: Boolean,
            author: String,
            payload: String,
            minutesAgo: Int,
        ): ProtocolVersion =
            ProtocolVersion(
                id = ProtocolVersionId(versionId),
                protocolId = ProtocolId(protocolId),
                // IMPORTANT: domain expects String
                version = versionNumber.toString(),
                published = published,
                createdAt = now.minus(minutesAgo.minutes),
                author = author,
                payload = payload,
            )

        fun protocol(
            id: String,
            name: String,
            summary: String,
            versions: List<ProtocolVersion>,
        ): Protocol {
            val latest = versions.maxByOrNull { it.version.toIntOrNull() ?: 0 }
            return Protocol(
                id = ProtocolId(id),
                name = name,
                summary = summary,
                versions = versions,
                // IMPORTANT: domain requires latestVersion
                latestVersion = latest,
            )
        }

        val p1Id = "proto-regen-001"
        val p2Id = "proto-qc-002"
        val p3Id = "proto-chain-003"

        return listOf(
            protocol(
                id = p1Id,
                name = "RegenOps: Controlled Growth Run",
                summary = "Execute a controlled growth simulation with safety bounds and trace checkpoints.",
                versions = listOf(
                    version(
                        p1Id, "pv-regen-001-3", 3, false, "ops@neogenesis",
                        """{"target":"growth","bounds":{"tempC":[31,36],"ph":[6.9,7.3]},"trace":"on"}""",
                        minutesAgo = 1
                    ),
                    version(
                        p1Id, "pv-regen-001-2", 2, true, "ops@neogenesis",
                        """{"target":"growth","bounds":{"tempC":[30,37],"ph":[6.8,7.4]},"trace":"on"}""",
                        minutesAgo = 12
                    ),
                ),
            ),
            protocol(
                id = p2Id,
                name = "QC: Parameter Drift Audit",
                summary = "Audit protocol focused on drift thresholds and evidence export integrity.",
                versions = listOf(
                    version(
                        p2Id, "pv-qc-002-1", 1, true, "qa@neogenesis",
                        """{"drift":{"temp":0.2,"ph":0.05},"export":"pdf"}""",
                        minutesAgo = 6
                    ),
                ),
            ),
            protocol(
                id = p3Id,
                name = "Chain-of-Evidence: Full Linkage",
                summary = "Enforce evidence hashing + manifest linkage at each stage transition.",
                versions = listOf(
                    version(
                        p3Id, "pv-chain-003-1", 1, true, "sec@neogenesis",
                        """{"hash":"sha256","bundle":"audit"}""",
                        minutesAgo = 30
                    ),
                ),
            ),
        )
    }
}