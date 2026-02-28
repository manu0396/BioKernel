package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.storage.HospitalIntegrations
import com.neogenesis.platform.core.storage.IntegrationEvents
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object HospitalIntegrationModule {
    @Serializable
    data class IntegrationEventRequest(val integrationId: String, val eventType: String, val payloadJson: String)

    fun register(app: Application) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/hospital") {
                    post("/events") {
                        if (!call.enforceRole(setOf("ADMIN", "RESEARCHER"))) return@post
                        val req = call.receive<IntegrationEventRequest>()
                        transaction {
                            IntegrationEvents.insert {
                                it[id] = UUID.randomUUID()
                                it[integrationId] = UUID.fromString(req.integrationId)
                                it[eventType] = req.eventType
                                it[payloadJson] = req.payloadJson
                                it[createdAt] = System.currentTimeMillis()
                            }
                        }
                        call.respond(HttpStatusCode.Accepted)
                    }
                    post("/status") {
                        if (!call.enforceRole(setOf("ADMIN", "RESEARCHER"))) return@post
                        call.respond(mapOf("status" to "ok"))
                    }
                }
            }
        }
    }
}

