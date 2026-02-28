package com.neogenesis.platform.control.di

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.local.RegenOpsLocalDataSource
import com.neogenesis.platform.control.data.oidc.OidcDeviceAuthService
import com.neogenesis.platform.control.data.remote.ControlApi
import com.neogenesis.platform.control.data.remote.DemoControlApi
import com.neogenesis.platform.control.data.remote.CommercialApi
import com.neogenesis.platform.control.data.remote.DemoExportsApi
import com.neogenesis.platform.control.data.remote.DemoTraceApi
import com.neogenesis.platform.control.data.remote.ExportsApi
import com.neogenesis.platform.control.data.remote.HttpCommercialApi
import com.neogenesis.platform.control.data.remote.HttpExportsApi
import com.neogenesis.platform.control.data.remote.HttpTraceApi
import com.neogenesis.platform.control.data.remote.TraceApi
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.control.data.stream.DemoStreamClient
import com.neogenesis.platform.control.data.stream.RegenOpsStreamClient
import com.neogenesis.platform.control.presentation.RegenOpsViewModel
import com.neogenesis.platform.control.data.db.RegenOpsDatabase
import com.neogenesis.platform.control.data.db.Protocol_versions
import com.neogenesis.platform.control.util.FileNameUtils
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.HttpClientFactory
import com.neogenesis.platform.shared.network.NetworkConfig
import com.neogenesis.platform.shared.network.NoOpLogger
import com.neogenesis.platform.shared.network.TokenStorage
import com.neogenesis.platform.shared.network.allowCleartextForLocalhost
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoin(appConfig: AppConfig, platformModule: Module): Koin {
    val existingKoin = GlobalContext.getOrNull()
    if (existingKoin != null) return existingKoin

    return startKoin {
        modules(commonModule(appConfig), platformModule, controlKoinModule)
    }.koin
}

val controlKoinModule: Module =
    module {
        single { FileNameUtils }
    }

fun commonModule(appConfig: AppConfig) = module {
    single { appConfig }
    single<AppLogger> { NoOpLogger }
    single {
        HttpClientFactory.create(
            NetworkConfig(
                baseUrl = appConfig.httpBaseUrl,
                allowCleartext = allowCleartextForLocalhost(appConfig.httpBaseUrl)
            ),
            tokenStorage = get<TokenStorage>()
        )
    }
    single {
        val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
            override fun decode(databaseValue: Long): Boolean = databaseValue == 1L
            override fun encode(value: Boolean): Long = if (value) 1L else 0L
        }
        RegenOpsDatabase(get<SqlDriver>(), Protocol_versions.Adapter(publishedAdapter = booleanAdapter))
    }
    single { RegenOpsLocalDataSource(get()) }
    single { OidcDeviceAuthService(get(), get()) }
    single { OidcRepository(get(), get(), get()) }
    single {
        if (appConfig.demoModeEnabled) {
            RegenOpsRepository(DemoControlApi(), get(), DemoStreamClient())
        } else {
            RegenOpsRepository(get<ControlApi>(), get(), get<RegenOpsStreamClient>())
        }
    }
    single<ExportsApi> {
        if (appConfig.demoModeEnabled) {
            DemoExportsApi()
        } else {
            HttpExportsApi(get())
        }
    }
    single<TraceApi> {
        if (appConfig.demoModeEnabled) {
            DemoTraceApi()
        } else {
            HttpTraceApi(get())
        }
    }
    single<CommercialApi> { HttpCommercialApi(get()) }
    single { RegenOpsViewModel(get(), get(), get(), get(), get(), get()) }
}
