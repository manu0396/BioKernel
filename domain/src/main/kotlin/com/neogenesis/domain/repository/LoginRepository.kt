package com.neogenesis.domain.repository

interface LoginRepository {
    suspend fun login(user: String, token: String): Boolean
}



