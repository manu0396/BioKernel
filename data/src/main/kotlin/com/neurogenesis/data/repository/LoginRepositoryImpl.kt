package com.neurogenesis.data.repository

import android.util.Log
import com.neurogenesis.data_core.network.KtorNeoService
import com.neurogenesis.domain.repository.LoginRepository

class LoginRepositoryImpl(
    private val api: KtorNeoService
) : LoginRepository {

    override suspend fun login(user: String, token: String): Boolean {
        return try {
            val response = api.authenticate(user, token)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("LoginRepositoryImpl", e.message.toString())
            false
        }
    }
}