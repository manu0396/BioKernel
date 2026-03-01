package com.neogenesis.platform.core.observability

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

object HttpRequestLabels {
    fun fromCall(call: ApplicationCall, protocolId: String? = null, protocolVersion: Int? = null): MetricLabels {
        val labels = MetricLabels(
            tenantId = call.request.header("X-Tenant-Id") ?: "unknown",
            siteId = call.request.header("X-Site-Id") ?: "unknown",
            cohortId = call.request.header("X-Cohort-Id") ?: "unknown",
            protocolId = call.request.header("X-Protocol-Id") ?: "unknown",
            protocolVersion = call.request.header("X-Protocol-Version") ?: "unknown"
        )
        return labels.withProtocol(protocolId, protocolVersion)
    }
}
