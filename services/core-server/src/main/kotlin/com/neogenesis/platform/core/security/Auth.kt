package com.neogenesis.platform.core.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth(jwtConfig: JwtConfig) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "neogenesis"
            verifier(jwtConfig.verifier())
            validate { credential ->
                val role = credential.payload.getClaim("role").asString()
                if (credential.payload.subject != null && role != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

