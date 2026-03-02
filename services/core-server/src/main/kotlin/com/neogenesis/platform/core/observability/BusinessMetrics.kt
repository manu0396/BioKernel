package com.neogenesis.platform.core.observability

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

private const val UNKNOWN = "unknown"

data class MetricLabels(
    val tenantId: String = UNKNOWN,
    val siteId: String = UNKNOWN,
    val cohortId: String = UNKNOWN,
    val protocolId: String = UNKNOWN,
    val protocolVersion: String = UNKNOWN
) {
    fun withProtocol(protocolId: String?, protocolVersion: Int?): MetricLabels = copy(
        protocolId = protocolId?.takeIf { it.isNotBlank() } ?: this.protocolId,
        protocolVersion = protocolVersion?.toString() ?: this.protocolVersion
    )
}

object BusinessMetrics {
    @Volatile
    private var registry: MeterRegistry? = null

    fun init(registry: MeterRegistry) {
        this.registry = registry
    }

    fun runStarted(labels: MetricLabels) {
        registry?.counter("neogenesis_runs_started_total", *labels.tags().toTypedArray())?.increment()
    }

    fun runCompleted(labels: MetricLabels) {
        registry?.counter("neogenesis_runs_completed_total", *labels.tags().toTypedArray())?.increment()
    }

    fun runFailed(labels: MetricLabels) {
        registry?.counter("neogenesis_runs_failed_total", *labels.tags().toTypedArray())?.increment()
    }

    fun runPaused(labels: MetricLabels) {
        registry?.counter("neogenesis_runs_paused_total", *labels.tags().toTypedArray())?.increment()
    }

    fun runResumed(labels: MetricLabels) {
        registry?.counter("neogenesis_runs_resumed_total", *labels.tags().toTypedArray())?.increment()
    }

    fun runRetried(labels: MetricLabels) {
        registry?.counter("neogenesis_runs_retry_total", *labels.tags().toTypedArray())?.increment()
    }

    fun runDuration(labels: MetricLabels, outcome: String, durationMs: Long) {
        val tags = labels.tags() + listOf("outcome", outcome)
        Timer.builder("neogenesis_run_duration_ms")
            .publishPercentileHistogram()
            .tags(*tags.toTypedArray())
            .register(registry ?: return)
            .record(durationMs, TimeUnit.MILLISECONDS)
    }

    fun evidenceExport(labels: MetricLabels, outcome: String) {
        registry?.counter("neogenesis_evidence_exports_total", *(labels.tags() + listOf("outcome", outcome)).toTypedArray())?.increment()
    }

    fun gatewayEvents(eventType: String, source: String) {
        registry?.counter(
            "neogenesis_gateway_events_total",
            *listOf("event_type", eventType, "source", source).toTypedArray()
        )?.increment()
    }

    fun telemetryFrames(metricKey: String) {
        registry?.counter("neogenesis_telemetry_frames_total", *listOf("metric_key", metricKey).toTypedArray())?.increment()
    }

    fun deviceCapabilityDecision(capability: String, outcome: String) {
        registry?.counter(
            "neogenesis_device_capability_total",
            *listOf("capability", capability, "outcome", outcome).toTypedArray()
        )?.increment()
    }

    private fun MetricLabels.tags(): List<String> = listOf(
        "tenant_id", tenantId.ifBlank { UNKNOWN },
        "site_id", siteId.ifBlank { UNKNOWN },
        "cohort_id", cohortId.ifBlank { UNKNOWN },
        "protocol_id", protocolId.ifBlank { UNKNOWN },
        "protocol_version", protocolVersion.ifBlank { UNKNOWN }
    )
}
