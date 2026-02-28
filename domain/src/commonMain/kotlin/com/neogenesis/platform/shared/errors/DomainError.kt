package com.neogenesis.platform.shared.errors

sealed class DomainError(val message: String) {
    class ValidationError(message: String) : DomainError(message)
    class NotFound(message: String) : DomainError(message)
    class Unauthorized(message: String) : DomainError(message)
    class Conflict(message: String) : DomainError(message)
    class Internal(message: String) : DomainError(message)
}

sealed class DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>()
    data class Failure(val error: DomainError) : DomainResult<Nothing>()
}
