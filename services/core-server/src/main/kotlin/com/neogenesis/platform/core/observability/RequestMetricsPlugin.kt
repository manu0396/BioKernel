package com.neogenesis.platform.core.observability

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import kotlin.math.max

class RequestMetricsConfig {
    var metrics: Metrics = NoopMetrics()
}

val RequestMetricsPlugin = createApplicationPlugin(
    name = "RequestMetricsPlugin",
    createConfiguration = ::RequestMetricsConfig
) {
    val registry = pluginConfig.metrics
    val startKey = AttributeKey<Long>("requestStartNs")
    onCall { call ->
        call.attributes.put(startKey, System.nanoTime())
    }
    onCallRespond { call, _ ->
        val start = call.attributes.getOrNull(startKey) ?: System.nanoTime()
        val durationMs = max(0, (System.nanoTime() - start) / 1_000_000)
        val statusCode = call.response.status()?.value ?: 0
        registry.recordRequest(call.request.path(), call.request.httpMethod.value, statusCode, durationMs)
    }
}

fun Application.installRequestMetrics(metrics: Metrics) {
    install(RequestMetricsPlugin) {
        this.metrics = metrics
    }
}

