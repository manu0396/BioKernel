package com.neogenesis.platform.backend.modules

import io.ktor.server.application.*
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

object HealthModule {
    fun register(app: Application) {
        app.routing {
            get("/health") { call.respond(mapOf("status" to "ok")) }
            get("/ready") { call.respond(mapOf("status" to "ready")) }
        }
    }
}
