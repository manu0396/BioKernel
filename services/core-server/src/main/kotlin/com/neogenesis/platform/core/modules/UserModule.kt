package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.core.http.respondError
import com.neogenesis.platform.core.security.enforceRole
import com.neogenesis.platform.core.security.requireCapability
import com.neogenesis.platform.core.storage.UserRepositoryImpl
import com.neogenesis.platform.shared.domain.Role
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.shared.domain.device.Capability
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

object UserModule {
    @Serializable
    data class RoleUpdateRequest(val userId: String, val role: Role)

    fun register(
        app: Application,
        userRepository: UserRepositoryImpl,
        auditLogger: AuditLogger,
        policyRepository: DevicePolicyRepository
    ) {
        app.routing {
            authenticate("auth-jwt") {
                route("/api/v1/users") {
                    get("/{id}") {
                        if (!call.enforceRole(setOf("ADMIN"))) return@get
                        val id = call.parameters["id"] ?: return@get call.respondError(
                            HttpStatusCode.BadRequest,
                            "missing_user_id",
                            "Missing user id"
                        )
                        val user = userRepository.findById(UserId(id)) ?: return@get call.respondError(
                            HttpStatusCode.NotFound,
                            "user_not_found",
                            "User not found"
                        )
                        call.respond(user)
                    }
                    post("/assign-role") {
                        if (!call.enforceRole(setOf("ADMIN"))) return@post
                        if (!call.requireCapability(Capability.ADMIN_SETTINGS, policyRepository, auditLogger)) return@post
                        val req = call.receive<RoleUpdateRequest>()
                        userRepository.assignRole(UserId(req.userId), req.role)
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/revoke-role") {
                        if (!call.enforceRole(setOf("ADMIN"))) return@post
                        if (!call.requireCapability(Capability.ADMIN_SETTINGS, policyRepository, auditLogger)) return@post
                        val req = call.receive<RoleUpdateRequest>()
                        userRepository.revokeRole(UserId(req.userId), req.role)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}

