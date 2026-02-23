package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.ActivateRecipeRequestDto
import com.neogenesis.platform.data.api.CreateRecipeRequestDto
import com.neogenesis.platform.data.api.UpdateRecipeRequestDto
import com.neogenesis.platform.shared.domain.Recipe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

interface RecipeApi {
    suspend fun list(): ApiResult<List<Recipe>>
    suspend fun create(name: String, description: String?, parameters: Map<String, String>): ApiResult<Recipe>
    suspend fun update(id: String, name: String, description: String?, parameters: Map<String, String>, active: Boolean): ApiResult<Recipe>
    suspend fun activate(id: String, active: Boolean): ApiResult<Unit>
}

class KtorRecipeApi(
    private val client: HttpClient
) : RecipeApi {
    override suspend fun list(): ApiResult<List<Recipe>> =
        safeApiCall { client.get("/api/v1/recipes") }

    override suspend fun create(
        name: String,
        description: String?,
        parameters: Map<String, String>
    ): ApiResult<Recipe> {
        val request = CreateRecipeRequestDto(name, description, parameters)
        return safeApiCall { client.post("/api/v1/recipes") { setBody(request) } }
    }

    override suspend fun update(
        id: String,
        name: String,
        description: String?,
        parameters: Map<String, String>,
        active: Boolean
    ): ApiResult<Recipe> {
        val request = UpdateRecipeRequestDto(name, description, parameters, active)
        return safeApiCall { client.put("/api/v1/recipes/$id") { setBody(request) } }
    }

    override suspend fun activate(id: String, active: Boolean): ApiResult<Unit> {
        val request = ActivateRecipeRequestDto(active)
        return safeApiCall { client.post("/api/v1/recipes/$id/activate") { setBody(request) } }
    }
}
