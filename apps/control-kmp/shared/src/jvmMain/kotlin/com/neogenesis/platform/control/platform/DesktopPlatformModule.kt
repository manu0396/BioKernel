package com.neogenesis.platform.control.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
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
import java.io.File

fun desktopPlatformModule(appConfig: AppConfig): Module = module {
    single<AppLogger>(override = true) { DesktopAppLogger() }
    single<TokenStorage> { DesktopTokenStorage(get()) }
    single<SqlDriver> {
        val dbFile = File(System.getProperty("user.home"), ".regenops/regenops.db")
        dbFile.parentFile?.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (!dbFile.exists()) {
            RegenOpsDatabase.Schema.create(driver)
        }
        driver
    }
    single<ControlApi> {
        if (appConfig.grpcHost.isBlank()) {
            HttpControlApi(get())
        } else {
            GrpcControlApi(appConfig)
        }
    }
    single<RegenOpsStreamClient> { GrpcRegenOpsStreamClient(appConfig, get(), get()) }
}
