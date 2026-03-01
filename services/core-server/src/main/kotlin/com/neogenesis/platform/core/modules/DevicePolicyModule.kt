package com.neogenesis.platform.core.modules

import com.neogenesis.platform.core.device.DevicePolicyRepository
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

object DevicePolicyModule {
    fun register(app: Application, repository: DevicePolicyRepository) {
        app.routing {
            get("/api/v1/device-policy") {
                call.respond(repository.load())
            }
            post("/api/v1/device/register") {
                call.receive<DeviceInfo>() // may be stored later
                call.respond(repository.load())
            }
        }
    }
}
