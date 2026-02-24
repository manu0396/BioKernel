package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.audit.AuditLogger
import com.neogenesis.platform.core.http.respondError
import com.neogenesis.platform.core.security.JwtConfig
import com.neogenesis.platform.core.security.PasswordHasher
import com.neogenesis.platform.core.security.RateLimiter
import com.neogenesis.platform.core.security.TokenStore
import com.neogenesis.platform.core.storage.SystemIds
import com.neogenesis.platform.core.storage.UserRepositoryImpl
import com.neogenesis.platform.shared.domain.Role
import com.neogenesis.platform.shared.domain.User
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.UserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

object AuthModule {
    @Serializable
    data class LoginRequest(val username: String, val password: String)

    @Serializable
    data class RegisterRequest(val username: String, val password: String, val roles: Set<Role>)

    @Serializable
    data class RefreshRequest(val refreshToken: String)

    @Serializable
    data class LogoutRequest(val refreshToken: String)

    @Serializable
    data class TokenResponse(val accessToken: String, val refreshToken: String)

    fun register(
        app: Application,
        jwtConfig: JwtConfig,
        userRepository: UserRepositoryImpl,
        tokenStore: TokenStore,
        auditLogger: AuditLogger
    ) {
        val limiter = RateLimiter(windowMs = 60_000, maxRequests = 20)
        app.routing {
            route("/api/v1/auth") {
                post("/register") {
                    val key = call.request.headers["X-Forwarded-For"] ?: "unknown"
                    if (!limiter.allow(key)) {
                        call.respondError(HttpStatusCode.TooManyRequests, "rate_limited", "Rate limit exceeded")
                        return@post
                    }
                    val req = call.receive<RegisterRequest>()
                    if (req.username.isBlank() || req.password.length < 8) {
                        call.respondError(HttpStatusCode.BadRequest, "invalid_registration", "Invalid registration")
                        return@post
                    }
                    val hash = PasswordHasher.hash(req.password)
                    val user = User(
                        id = UserId(UUID.randomUUID().toString()),
                        username = req.username,
                        roles = req.roles.ifEmpty { setOf(Role.OPERATOR) },
                        active = true,
                        createdAt = Clock.System.now()
                    )
                    val created = userRepository.create(user, hash)
                    req.roles.forEach { userRepository.assignRole(created.id, it) }
                    auditLogger.appendEvent(
                        jobId = PrintJobId(SystemIds.jobId.toString()),
                        actorId = created.id,
                        deviceId = DeviceId(SystemIds.deviceId.toString()),
                        eventType = "USER_REGISTERED",
                        payload = "{\"userId\":\"${created.id.value}\",\"username\":\"${created.username}\"}"
                    )
                    call.respond(HttpStatusCode.Created, created)
                }

                post("/login") {
                    val key = call.request.headers["X-Forwarded-For"] ?: "unknown"
                    if (!limiter.allow(key)) {
                        call.respondError(HttpStatusCode.TooManyRequests, "rate_limited", "Rate limit exceeded")
                        return@post
                    }
                    val req = call.receive<LoginRequest>()
                    val hash = userRepository.findPasswordHash(req.username)
                    val user = userRepository.findByUsername(req.username)
                    if (hash == null || user == null || !PasswordHasher.verify(req.password, hash)) {
                        call.respondError(HttpStatusCode.Unauthorized, "invalid_credentials", "Invalid credentials")
                        return@post
                    }
                    val roles = userRepository.rolesForUser(user.id)
                    val role = roles.firstOrNull() ?: Role.OPERATOR
                    val access = jwtConfig.makeAccessToken(user.id.value, role.name)
                    val refresh = jwtConfig.makeRefreshToken(user.id.value, role.name)
                    auditLogger.appendEvent(
                        jobId = PrintJobId(SystemIds.jobId.toString()),
                        actorId = user.id,
                        deviceId = DeviceId(SystemIds.deviceId.toString()),
                        eventType = "USER_LOGIN",
                        payload = "{\"userId\":\"${user.id.value}\",\"username\":\"${user.username}\"}"
                    )
                    call.respond(TokenResponse(access, refresh))
                }

                post("/refresh") {
                    val req = call.receive<RefreshRequest>()
                    if (tokenStore.isRevoked(req.refreshToken)) {
                        call.respondError(HttpStatusCode.Unauthorized, "refresh_revoked", "Refresh token revoked")
                        return@post
                    }
                    val verifier = jwtConfig.verifier()
                    val decoded = verifier.verify(req.refreshToken)
                    val tokenType = decoded.getClaim("type").asString()
                    if (tokenType != "refresh") {
                        call.respondError(HttpStatusCode.Unauthorized, "invalid_refresh", "Invalid refresh token")
                        return@post
                    }
                    val subject = decoded.subject
                    val role = decoded.getClaim("role").asString() ?: Role.OPERATOR.name
                    val access = jwtConfig.makeAccessToken(subject, role)
                    val refresh = jwtConfig.makeRefreshToken(subject, role)
                    call.respond(TokenResponse(access, refresh))
                }

                post("/logout") {
                    val req = call.receive<LogoutRequest>()
                    val verifier = jwtConfig.verifier()
                    val decoded = verifier.verify(req.refreshToken)
                    val exp = decoded.expiresAt.time
                    tokenStore.revoke(req.refreshToken, exp)
                    auditLogger.appendEvent(
                        jobId = PrintJobId(SystemIds.jobId.toString()),
                        actorId = UserId(decoded.subject),
                        deviceId = DeviceId(SystemIds.deviceId.toString()),
                        eventType = "USER_LOGOUT",
                        payload = "{\"userId\":\"${decoded.subject}\"}"
                    )
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

