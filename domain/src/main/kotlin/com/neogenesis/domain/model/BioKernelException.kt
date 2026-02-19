package com.neogenesis.domain.model

sealed class BioKernelException(message: String) : Exception(message) {
    class UnauthorizedException : BioKernelException("La sesión ha expirado o el token es inválido.")
    class NoNetworkException : BioKernelException("No se pudo establecer conexión con el servidor.")
    data class ServerException(val code: Int, val serverMessage: String) :
        BioKernelException("Error del servidor ($code): $serverMessage")
}