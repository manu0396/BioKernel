package com.neogenesis.platform.core.observability

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

interface Metrics {
    fun recordRequest(path: String, method: String, status: Int, durationMs: Long)
}

class NoopMetrics : Metrics {
    override fun recordRequest(path: String, method: String, status: Int, durationMs: Long) = Unit
}

class PrometheusRequestMetrics(private val registry: MeterRegistry) : Metrics {
    override fun recordRequest(path: String, method: String, status: Int, durationMs: Long) {
        val tags = listOf(
            "path", path,
            "method", method,
            "status", status.toString()
        )
        registry.counter("neogenesis_http_requests_total", *tags.toTypedArray()).increment()
        Timer.builder("neogenesis_http_request_duration_ms")
            .publishPercentileHistogram()
            .tags(*tags.toTypedArray())
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS)
    }
}
