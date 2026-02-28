package com.neogenesis.platform.backend.http

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.response.respond

suspend fun ApplicationCall.respondError(
    status: HttpStatusCode,
    code: String,
    message: String
) {
    respond(status, ErrorResponse(code = code, message = message, correlationId = callId))
}
