package com.neogenesis.platform.backend.security

import com.neogenesis.platform.backend.http.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal

fun ApplicationCall.requireRole(allowed: Set<String>): Boolean {
    val principal = this.principal<JWTPrincipal>() ?: return false
    val role = principal.payload.getClaim("role").asString()
    return role in allowed
}

fun ApplicationCall.jwtSubject(): String? =
    principal<JWTPrincipal>()?.payload?.subject

suspend fun ApplicationCall.enforceRole(allowed: Set<String>): Boolean {
    if (!requireRole(allowed)) {
        respondError(HttpStatusCode.Forbidden, "forbidden", "Insufficient role")
        return false
    }
    return true
}
