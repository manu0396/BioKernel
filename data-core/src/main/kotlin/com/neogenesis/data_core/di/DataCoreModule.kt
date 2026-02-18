package com.neogenesis.data_core.di

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.neogenesis.data_core.network.KtorNeoService
import com.neogenesis.data_core.network.KtorNeoServiceImpl
import com.neogenesis.data_core.persistence.*
import com.neogenesis.datacore.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val commonJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
    encodeDefaults = true
}

val dataCoreModule = module {
    single {
        val shouldMock = BuildConfig.DEBUG && (BuildConfig.FLAVOR.contains("mock", true) || true)

        if (shouldMock) {
            createMockKtorClient()
        } else {
            createRealKtorClient()
        }
    }

    single {
        KtorNeoService(
            client = get(),
            baseUrl = "http://10.0.2.2:8080"
        )
    }
    single<SecureKeyManager> { SecureKeyManagerImpl(androidContext()) }
    single { EncryptedDriverFactory(androidContext()) }

    factory<BiometricAuthManager> { (activity: FragmentActivity) ->
        BiometricAuthManagerImpl(activity)
    }
}

private fun HttpClientConfig<*>.installStandardFeatures() {
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
}

fun createRealKtorClient(): HttpClient = HttpClient(OkHttp) {
    installStandardFeatures()
}

fun createMockKtorClient(): HttpClient {
    return HttpClient(MockEngine) {
        installStandardFeatures()

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