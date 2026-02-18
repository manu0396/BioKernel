package com.neogenesis.domain.usecases

import com.neogenesis.domain.model.SessionMetadata
import com.neogenesis.domain.model.User
import com.neogenesis.domain.repository.LoginRepository
import kotlinx.coroutines.flow.Flow

class LoginUseCase(private val repository: LoginRepository) {
    suspend operator fun invoke(user: String, pass: String): Result<User> {
        if (user.isBlank() || pass.length < 6) {
            throw Exception("Invalid credentials for NeoGenesis access")
        }
        return repository.login(user, pass)
    }
    fun getSessionMetadata(): Flow<SessionMetadata?> = repository.getSessionMetadata()
}






