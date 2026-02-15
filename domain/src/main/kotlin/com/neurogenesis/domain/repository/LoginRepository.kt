package com.neurogenesis.domain.repository

interface LoginRepository {
    suspend fun login(user: String, token: String): Boolean
}