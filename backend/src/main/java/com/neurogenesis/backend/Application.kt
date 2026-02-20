package com.neurogenesis.backend

import com.neurogenesis.shared_network.models.RetinaSampleDto
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import com.neurogenesis.shared_network.models.LoginRequest
import com.neurogenesis.shared_network.models.LoginResponse

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        post("/login") {
            val request = call.receive<LoginRequest>()
            if (request.pass == "123456") {
                call.respond(
                    LoginResponse(
                        true,
                        "secure_token_esp_2026",
                        "BioKernel España: Acceso garantizado"
                    )
                )
            } else {
                call.respond(LoginResponse(false, "", "Credenciales inválidas"))
            }
        }

        get("/retina/samples") {
            val samples = listOf(
                RetinaSampleDto("ESP-NODO-MAD-01", "SYSTEM", 0.08, "2026-02-19T10:00:00Z"),
                RetinaSampleDto("ESP-NODO-BCN-02", "SYSTEM", 0.14, "2026-02-19T12:30:00Z"),
                RetinaSampleDto("ESP-ALERTA-LETHAL", "SYSTEM", 0.95, "2026-02-19T14:15:00Z")
            )
            call.respond(samples)
        }
    }
}