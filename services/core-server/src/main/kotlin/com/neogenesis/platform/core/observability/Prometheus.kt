package com.neogenesis.platform.core.observability

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.response.respondText
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.installPrometheusMetrics(): PrometheusMeterRegistry {
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    routing {
        get("/metrics") {
            call.respondText(registry.scrape())
        }
    }
    return registry
}
