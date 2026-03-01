package com.neogenesis.platform.core.modules

import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

object AdminOpsModule {
    fun register(app: Application) {
        app.routing {
            get("/admin/metrics") {
                call.respond(
                    mapOf(
                        "uptime" to System.currentTimeMillis(),
                        "status" to "ok"
                    )
                )
            }
            get("/openapi.json") {
                val url = app.environment.classLoader.getResource("openapi.json")
                    ?: return@get call.respond(mapOf("error" to "missing openapi"))
                val text = url.openStream().bufferedReader().readText()
                call.respondText(text, io.ktor.http.ContentType.Application.Json)
            }
        }
    }
}

