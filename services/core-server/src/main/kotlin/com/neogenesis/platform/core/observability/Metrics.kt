package com.neogenesis.platform.core.observability

interface Metrics {
    fun recordRequest(path: String, method: String, status: Int, durationMs: Long)
}

class NoopMetrics : Metrics {
    override fun recordRequest(path: String, method: String, status: Int, durationMs: Long) = Unit
}

