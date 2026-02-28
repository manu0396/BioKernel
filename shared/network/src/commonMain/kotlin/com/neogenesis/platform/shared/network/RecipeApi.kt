package com.neogenesis.platform.shared.network

import com.neogenesis.platform.data.api.ActivateRecipeRequestDto
import com.neogenesis.platform.data.api.CreateRecipeRequestDto
import com.neogenesis.platform.data.api.UpdateRecipeRequestDto
import com.neogenesis.platform.shared.domain.Recipe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
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
    private val client: HttpClient,
    private val logger: AppLogger = NoOpLogger
) : RecipeApi {
    override suspend fun list(): ApiResult<List<Recipe>> =
        run {
            val correlationId = CorrelationIds.newId()
            safeApiCall(logger, correlationId) {
                client.get("/api/v1/recipes") { header("X-Correlation-Id", correlationId) }
            }
        }

    override suspend fun create(
        name: String,
        description: String?,
        parameters: Map<String, String>
    ): ApiResult<Recipe> {
        val request = CreateRecipeRequestDto(name, description, parameters)
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/recipes") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }

    override suspend fun update(
        id: String,
        name: String,
        description: String?,
        parameters: Map<String, String>,
        active: Boolean
    ): ApiResult<Recipe> {
        val request = UpdateRecipeRequestDto(name, description, parameters, active)
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.put("/api/v1/recipes/$id") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }

    override suspend fun activate(id: String, active: Boolean): ApiResult<Unit> {
        val request = ActivateRecipeRequestDto(active)
        val correlationId = CorrelationIds.newId()
        return safeApiCall(logger, correlationId) {
            client.post("/api/v1/recipes/$id/activate") {
                header("X-Correlation-Id", correlationId)
                setBody(request)
            }
        }
    }
}
