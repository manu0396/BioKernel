package com.neogenesis.domain.usecases

import com.neogenesis.domain.repository.LoginRepository

class LoginUseCase(private val repository: LoginRepository) {
    /**
     * Executes the authentication logic for the NeoGenesis platform.
     * Currently focused on the Spain production environment.
     */
    suspend operator fun invoke(user: String, pass: String): Boolean {
        // Validation logic
        if (user.isBlank() || pass.length < 6) {
            throw Exception("Invalid credentials for NeoGenesis access")
        }

        // Delegating to repository for the actual network/data operation
        return repository.login(user, pass)
    }
}



