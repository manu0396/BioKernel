package com.neogenesis.platform.backend.modules

import com.neogenesis.platform.backend.audit.AuditLogger
import com.neogenesis.platform.backend.security.enforceRole
import com.neogenesis.platform.backend.security.jwtSubject
import com.neogenesis.platform.backend.storage.RecipeRepositoryImpl
import com.neogenesis.platform.backend.storage.SystemIds
import com.neogenesis.platform.shared.domain.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

object RecipeModule {
    @Serializable
    data class CreateRecipeRequest(
        val name: String,
        val description: String?,
        val parameters: Map<String, String>
    )

    @Serializable
    data class UpdateRecipeRequest(
        val name: String,
        val description: String?,
        val parameters: Map<String, String>,
        val active: Boolean
    )

    @Serializable
    data class ActivateRecipeRequest(
        val active: Boolean = true
    )

    fun register(
        app: Application,
        repository: RecipeRepositoryImpl,
        auditLogger: AuditLogger
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/recipes") {
                    get {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER", "AUDITOR"))) return@get
                        call.respond(repository.list())
                    }
                    post {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER"))) return@post
                        val req = call.receive<CreateRecipeRequest>()
                        val now = Clock.System.now()
                        val recipe = Recipe(
                            id = RecipeId(UUID.randomUUID().toString()),
                            name = req.name,
                            description = req.description,
                            parameters = req.parameters,
                            active = false,
                            createdAt = now,
                            updatedAt = now
                        )
                        val created = repository.create(recipe)
                        val actorId = call.jwtSubject()?.let { UserId(it) } ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(SystemIds.jobId.toString()),
                            actorId = actorId,
                            deviceId = DeviceId(SystemIds.deviceId.toString()),
                            eventType = "RECIPE_CREATED",
                            payload = "{\"recipeId\":\"${created.id.value}\",\"name\":\"${created.name}\"}"
                        )
                        call.respond(HttpStatusCode.Created, created)
                    }
                    put("/{id}") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER"))) return@put
                        val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                        val existing = repository.findById(RecipeId(id))
                            ?: return@put call.respond(HttpStatusCode.NotFound)
                        val req = call.receive<UpdateRecipeRequest>()
                        val now = Clock.System.now()
                        val recipe = Recipe(
                            id = RecipeId(id),
                            name = req.name,
                            description = req.description,
                            parameters = req.parameters,
                            active = req.active,
                            createdAt = existing.createdAt,
                            updatedAt = now
                        )
                        val updated = repository.update(recipe)
                        val actorId = call.jwtSubject()?.let { UserId(it) } ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(SystemIds.jobId.toString()),
                            actorId = actorId,
                            deviceId = DeviceId(SystemIds.deviceId.toString()),
                            eventType = "RECIPE_UPDATED",
                            payload = "{\"recipeId\":\"${updated.id.value}\",\"active\":${updated.active}}"
                        )
                        call.respond(HttpStatusCode.OK, updated)
                    }
                    post("/{id}/activate") {
                        if (!call.enforceRole(setOf("ADMIN", "OPERATOR", "RESEARCHER"))) return@post
                        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val req = call.receive<ActivateRecipeRequest>()
                        repository.findById(RecipeId(id))
                            ?: return@post call.respond(HttpStatusCode.NotFound)
                        repository.setActive(RecipeId(id), req.active)
                        val actorId = call.jwtSubject()?.let { UserId(it) } ?: UserId(SystemIds.userId.toString())
                        auditLogger.appendEvent(
                            jobId = PrintJobId(SystemIds.jobId.toString()),
                            actorId = actorId,
                            deviceId = DeviceId(SystemIds.deviceId.toString()),
                            eventType = "RECIPE_ACTIVATED",
                            payload = "{\"recipeId\":\"$id\",\"active\":${req.active}}"
                        )
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}
