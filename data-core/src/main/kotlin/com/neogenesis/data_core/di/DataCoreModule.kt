package com.neogenesis.data_core.di

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.neogenesis.data_core.network.BioApiService
import com.neogenesis.data_core.persistence.BiometricAuthManager
import com.neogenesis.data_core.persistence.BiometricAuthManagerImpl
import com.neogenesis.data_core.persistence.CryptoManager
import com.neogenesis.data_core.persistence.CryptoManagerImpl
import com.neogenesis.data_core.persistence.EncryptedDriverFactory
import com.neogenesis.data_core.persistence.SecureKeyManager
import com.neogenesis.data_core.persistence.SecureKeyManagerImpl
import com.neogenesis.data_core.worker.HazardWorker
import com.neogenesis.datacore.BuildConfig
import com.neogenesis.domain.model.BioKernelException
import com.neogenesis.domain.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module

private val commonJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
    encodeDefaults = true
}

val dataCoreModule = module {
    single<HttpClient> {
        val sessionManager: SessionManager = get()
        val isMock = BuildConfig.DEBUG && BuildConfig.FLAVOR.contains("mock", true)
        if (isMock) createMockKtorClient(sessionManager) else createRealKtorClient(sessionManager)
    }

    single {
        BioApiService(
            client = get(),
            baseUrl = BuildConfig.BASE_URL
        )
    }
    single<SecureKeyManager> { SecureKeyManagerImpl(androidContext()) }
    single { EncryptedDriverFactory(androidContext()) }

    factory<BiometricAuthManager> { (activity: FragmentActivity) ->
        BiometricAuthManagerImpl(activity)
    }
    single<CryptoManager> { CryptoManagerImpl() }

    worker { HazardWorker(get(), get(), get()) }
}

private fun HttpClientConfig<*>.installStandardFeatures(sessionManager: SessionManager) {
    install(ContentNegotiation) { json(commonJson) }
    install(Logging) {
        logger = object : Logger { override fun log(message: String) { Log.d("KtorHTTP", message) } }
        level = LogLevel.ALL
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15000L
        connectTimeoutMillis = 10000L
    }
    defaultRequest {
        url(BuildConfig.BASE_URL)
        contentType(ContentType.Application.Json)
    }

    HttpResponseValidator {
        validateResponse { response ->
            val status = response.status
            val code = status.value
            if (code in 200..299) return@validateResponse
            when (code) {
                401 -> {
                    sessionManager.clear()
                    throw BioKernelException.UnauthorizedException()
                }
                403 -> throw BioKernelException.ServerException(code, "Acceso restringido al Bio-Nodo.")
                404 -> throw BioKernelException.ServerException(code, "Recurso no encontrado.")
                409 -> throw BioKernelException.ServerException(code, "Conflicto: La muestra ya existe.")
                in 500..599 -> {
                    throw BioKernelException.ServerException(code, "Error crítico en la infraestructura de BioKernel.")
                }
                else -> {
                    throw BioKernelException.ServerException(code, "Error de red inesperado.")
                }
            }
        }
    }
}

fun createRealKtorClient(sessionManager: SessionManager): HttpClient = HttpClient(OkHttp) {
    installStandardFeatures(sessionManager)
}

fun createMockKtorClient(sessionManager: SessionManager): HttpClient {
    return HttpClient(MockEngine) {
        installStandardFeatures(sessionManager)

        engine {
            addHandler { request ->
                val path = request.url.encodedPath
                val responseHeaders = headersOf(HttpHeaders.ContentType, "application/json")

                when {
                    path.contains("login") -> respond(
                        content = """{"success": true, "token": "mock_2026", "message": "OK"}""",
                        status = HttpStatusCode.OK,
                        headers = responseHeaders
                    )

                    path.contains("retina/samples") -> respond(
                        content = createToxicitySamplesJson(),
                        status = HttpStatusCode.OK,
                        headers = responseHeaders
                    )

                    else -> respond(
                        content = """{"success": false, "message": "Not Found"}""",
                        status = HttpStatusCode.NotFound,
                        headers = responseHeaders
                    )
                }
            }
        }
    }
}

private fun createToxicitySamplesJson(): String = """
[
    {"id":"L1","patientId":"P01","toxicityScore":0.05,"timestamp":"2026-02-18T08:00Z"},
    {"id":"L2","patientId":"P01","toxicityScore":0.12,"timestamp":"2026-02-18T08:30Z"},
    {"id":"L3","patientId":"P02","toxicityScore":0.18,"timestamp":"2026-02-18T09:00Z"},
    {"id":"L4","patientId":"P02","toxicityScore":0.22,"timestamp":"2026-02-18T09:15Z"},
    
    {"id":"M1","patientId":"P03","toxicityScore":0.35,"timestamp":"2026-02-18T10:00Z"},
    {"id":"M2","patientId":"P03","toxicityScore":0.42,"timestamp":"2026-02-18T10:15Z"},
    {"id":"M3","patientId":"P04","toxicityScore":0.45,"timestamp":"2026-02-18T10:30Z"},
    {"id":"M4","patientId":"P04","toxicityScore":0.48,"timestamp":"2026-02-18T10:45Z"},
    
    {"id":"H1","patientId":"P05","toxicityScore":0.55,"timestamp":"2026-02-18T11:00Z"},
    {"id":"H2","patientId":"P05","toxicityScore":0.62,"timestamp":"2026-02-18T11:15Z"},
    {"id":"H3","patientId":"P06","toxicityScore":0.68,"timestamp":"2026-02-18T11:30Z"},
    {"id":"H4","patientId":"P06","toxicityScore":0.72,"timestamp":"2026-02-18T11:45Z"},
    
    {"id":"C1","patientId":"P07","toxicityScore":0.82,"timestamp":"2026-02-18T12:00Z"},
    {"id":"C2","patientId":"P07","toxicityScore":0.88,"timestamp":"2026-02-18T12:15Z"},
    {"id":"C3","patientId":"P08","toxicityScore":0.95,"timestamp":"2026-02-18T12:30Z"},
    {"id":"C4","patientId":"P08","toxicityScore":0.99,"timestamp":"2026-02-18T12:45Z"}
]
""".trimIndent()