package com.neogenesis.platform.backend.validation

import com.neogenesis.platform.backend.modules.AuthModule
import com.neogenesis.platform.backend.modules.DeviceModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<AuthModule.LoginRequest> { req ->
            when {
                req.username.isBlank() -> ValidationResult.Invalid("username_required")
                req.password.length < 8 -> ValidationResult.Invalid("password_too_short")
                else -> ValidationResult.Valid
            }
        }
        validate<AuthModule.RegisterRequest> { req ->
            when {
                req.username.isBlank() -> ValidationResult.Invalid("username_required")
                req.password.length < 8 -> ValidationResult.Invalid("password_too_short")
                else -> ValidationResult.Valid
            }
        }
        validate<AuthModule.RefreshRequest> { req ->
            if (req.refreshToken.isBlank()) ValidationResult.Invalid("refresh_token_required") else ValidationResult.Valid
        }
        validate<AuthModule.LogoutRequest> { req ->
            if (req.refreshToken.isBlank()) ValidationResult.Invalid("refresh_token_required") else ValidationResult.Valid
        }
        validate<DeviceModule.DeviceRegisterRequest> { req ->
            when {
                req.serialNumber.isBlank() -> ValidationResult.Invalid("serial_number_required")
                req.firmwareVersion.isBlank() -> ValidationResult.Invalid("firmware_version_required")
                else -> ValidationResult.Valid
            }
        }
        validate<DeviceModule.PairCompleteRequest> { req ->
            when {
                req.pairingId.isBlank() -> ValidationResult.Invalid("pairing_id_required")
                req.response.isBlank() -> ValidationResult.Invalid("pairing_response_required")
                else -> ValidationResult.Valid
            }
        }
    }
}
