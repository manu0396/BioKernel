package com.neogenesis.platform.control.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.db.RegenOpsDatabase
import com.neogenesis.platform.control.data.remote.ControlApi
import com.neogenesis.platform.control.data.remote.GrpcControlApi
import com.neogenesis.platform.control.data.remote.HttpControlApi
import com.neogenesis.platform.control.data.stream.GrpcRegenOpsStreamClient
import com.neogenesis.platform.control.data.stream.RegenOpsStreamClient
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context, appConfig: AppConfig): Module = module {
    single<AppLogger>(override = true) { AndroidAppLogger() }
    single<TokenStorage> { AndroidTokenStorage(context) }
    single<SqlDriver> { AndroidSqliteDriver(RegenOpsDatabase.Schema, context, "regenops.db") }
    single<ControlApi> {
        if (appConfig.grpcHost.isBlank()) {
            HttpControlApi(get())
        } else {
            GrpcControlApi(appConfig)
        }
    }
    single<RegenOpsStreamClient> { GrpcRegenOpsStreamClient(appConfig, get(), get()) }
}
