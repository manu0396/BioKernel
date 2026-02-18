package com.neogenesis.mock

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class LoginRequest(
    val user: String,
    val pass: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
    val patientId: String? = null,
)

@Serializable
data class RetinaSampleDto(
    val id: String,
    val patientId: String,
    val toxicityScore: Double,
    val timestamp: String
)

fun main() {
    println("BioKernel Mock Server online at http://0.0.0.0:8080")

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                call.respond(
                    HttpStatusCode.InternalServerError,
                    LoginResponse(success = false, message = "Internal Error: ${cause.localizedMessage}")
                )
            }
        }

        routing {
            post("/login") {
                try {
                    val request = call.receive<LoginRequest>()
                    if (request.user == "manulucas0396@gmail.com" && request.pass == "123456") {
                        call.respond(
                            HttpStatusCode.OK,
                            LoginResponse(
                                success = true,
                                token = "bk_token_2026",
                                message = "Authenticated",
                                patientId = "P-7723"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            LoginResponse(success = false, message = "Credenciales incorrectas")
                        )
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, LoginResponse(false, message = "Payload inválido"))
                }
            }

            get("/retina/samples") {
                val samples = generateToxicityDataset()
                call.respond(HttpStatusCode.OK, samples)
            }

            get("/") {
                call.respondText("BioKernel Service Online", ContentType.Text.Plain)
            }
        }
    }.start(wait = true)
}

private fun generateToxicityDataset(): List<RetinaSampleDto> {
    val now = ZonedDateTime.now()
    val dataset = mutableListOf<RetinaSampleDto>()
    // Generate 16 samples (4 per category)
    for (i in 1..4) dataset.add(RetinaSampleDto("L-$i", "P-100", 0.05 * i, now.minusHours(i.toLong()).toIsoString()))
    for (i in 1..4) dataset.add(RetinaSampleDto("M-$i", "P-100", 0.25 + (0.06 * i), now.minusHours(i.toLong() + 4).toIsoString()))
    for (i in 1..4) dataset.add(RetinaSampleDto("H-$i", "P-100", 0.50 + (0.06 * i), now.minusHours(i.toLong() + 8).toIsoString()))
    for (i in 1..4) dataset.add(RetinaSampleDto("C-$i", "P-100", 0.75 + (0.06 * i), now.minusHours(i.toLong() + 12).toIsoString()))
    return dataset
}

private fun ZonedDateTime.toIsoString(): String = this.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)