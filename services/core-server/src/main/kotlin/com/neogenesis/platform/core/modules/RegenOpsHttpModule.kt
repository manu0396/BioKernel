package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

object RegenOpsHttpModule {
    @Serializable
    data class StartRunRequestDto(
        val protocolId: String,
        val protocolVersion: Int,
        val runId: String? = null,
        val gatewayId: String? = null
    )

    fun register(app: Application) {
        app.routing {
            route("/api/v1/regenops") {
                get("/protocols") {
                    call.respond(RegenOpsInMemoryStore.listProtocols())
                }
                post("/runs/start") {
                    val req = call.receive<StartRunRequestDto>()
                    val run = RegenOpsInMemoryStore.startRun(
                        protocolId = req.protocolId,
                        version = req.protocolVersion,
                        requestedRunId = req.runId.orEmpty(),
                        gatewayId = req.gatewayId ?: "gateway-http"
                    )
                    call.respond(HttpStatusCode.Created, run)
                }
            }
        }
    }
}
