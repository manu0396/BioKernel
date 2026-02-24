package com.neogenesis.platform.control.di

import app.cash.sqldelight.db.SqlDriver
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.local.RegenOpsLocalDataSource
import com.neogenesis.platform.control.data.oidc.OidcDeviceAuthService
import com.neogenesis.platform.control.data.remote.ControlApi
import com.neogenesis.platform.control.data.remote.CommercialApi
import com.neogenesis.platform.control.data.remote.HttpCommercialApi
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.control.data.stream.RegenOpsStreamClient
import com.neogenesis.platform.control.presentation.RegenOpsViewModel
import com.neogenesis.platform.control.data.db.RegenOpsDatabase
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.HttpClientFactory
import com.neogenesis.platform.shared.network.NetworkConfig
import com.neogenesis.platform.shared.network.NoOpLogger
import com.neogenesis.platform.shared.network.TokenStorage
import com.neogenesis.platform.shared.network.allowCleartextForLocalhost
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoin(appConfig: AppConfig, platformModule: Module): KoinApplication {
    GlobalContext.getOrNull()?.let { return it }
    return startKoin {
        modules(commonModule(appConfig), platformModule)
    }
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
    single { RegenOpsDatabase(get<SqlDriver>()) }
    single { RegenOpsLocalDataSource(get()) }
    single { OidcDeviceAuthService(get(), get()) }
    single { OidcRepository(get(), get(), get()) }
    single { RegenOpsRepository(get<ControlApi>(), get(), get<RegenOpsStreamClient>()) }
    single<CommercialApi> { HttpCommercialApi(get()) }
    single { RegenOpsViewModel(get(), get(), get(), get()) }
}
