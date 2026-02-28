package com.neogenesis.platform.backend.modules

import com.neogenesis.platform.backend.security.enforceRole
import com.neogenesis.platform.backend.storage.BioinkRepositoryImpl
import com.neogenesis.platform.shared.domain.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

object BioinkModule {
    @Serializable
    data class CreateProfileRequest(val name: String, val manufacturer: String?, val viscosityModel: String)

    @Serializable
    data class CreateBatchRequest(val profileId: String, val lotNumber: String, val manufacturer: String, val expiresAtMs: Long)

    fun register(app: Application, repository: BioinkRepositoryImpl) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/bioink") {
                    post("/profiles") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER"))) return@post
                        val req = call.receive<CreateProfileRequest>()
                        val profile = BioinkProfile(
                            id = BioinkProfileId(UUID.randomUUID().toString()),
                            name = req.name,
                            manufacturer = req.manufacturer,
                            viscosityModel = req.viscosityModel,
                            createdAt = Clock.System.now()
                        )
                        call.respond(HttpStatusCode.Created, repository.createProfile(profile))
                    }
                    get("/profiles") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                        call.respond(repository.listProfiles())
                    }
                    post("/batches") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR"))) return@post
                        val req = call.receive<CreateBatchRequest>()
                        val batch = BioinkBatch(
                            id = BioinkBatchId(UUID.randomUUID().toString()),
                            profileId = BioinkProfileId(req.profileId),
                            lotNumber = req.lotNumber,
                            manufacturer = req.manufacturer,
                            createdAt = Clock.System.now(),
                            expiresAt = kotlinx.datetime.Instant.fromEpochMilliseconds(req.expiresAtMs)
                        )
                        call.respond(HttpStatusCode.Created, repository.createBatch(batch))
                    }
                    get("/batches") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                        call.respond(repository.listBatches())
                    }
                }
            }
        }
    }
}
