package com.neogenesis.data_core.di

import com.neogenesis.data_core.network.KtorNeoService
import com.neogenesis.datacore.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val networkModule = module {
    single {
        if (BuildConfig.FLAVOR == "demo") {
            createMockKtorClient()
        } else {
            createRealKtorClient()
        }
    }
    singleOf(::KtorNeoService)
}

fun createRealKtorClient(): HttpClient {
    return HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }
}

fun createMockKtorClient(): HttpClient {
    val mockEngine = MockEngine { request ->
        val path = request.url.encodedPath

        if (path.contains("/sync") || path.contains("retina")) {
            respond(
                content = """
                    [
                        {
                            "sampleId": "BIO-${System.currentTimeMillis()}",
                            "compScore": 98.5,
                            "toxLevel": "LOW",
                            "toxValue": 0.1,
                            "createdAt": ${System.currentTimeMillis()},
                            "formattedDate": "2024-05-20 14:30",
                            "labNotes": "Simulated Connection Established",
                            "secureHash": "0xMOCK_HASH_123"
                        }
                    ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        } else {
            respondError(HttpStatusCode.NotFound, "Endpoint no simulado: $path")
        }
    }

    return HttpClient(mockEngine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
}