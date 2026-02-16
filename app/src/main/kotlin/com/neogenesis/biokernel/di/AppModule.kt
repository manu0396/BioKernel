package com.neogenesis.biokernel.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.neogenesis.data.db.BioKernelDatabase
import com.neogenesis.data.repository.RetinaRepositoryImpl
import com.neogenesis.data_core.network.KtorNeoService
import com.neogenesis.domain.repository.RetinaRepository
import com.neogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.LoginUseCase
import com.neogenesis.feature_login.presentation.LoginViewModel
import feature_dashboard.ui.viewmodel.DashboardViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Infrastructure
    single { HttpClient { install(ContentNegotiation) { json() } } }

    // Database
    single {
        val driver = AndroidSqliteDriver(BioKernelDatabase.Schema, androidContext(), "biokernel.db")
        BioKernelDatabase(driver)
    }

    // Data & Domain
    singleOf(::KtorNeoService)
    single<RetinaRepository> { RetinaRepositoryImpl(get(), get()) }

    factoryOf(::LoginUseCase)
    factoryOf(::GetRetinaAnalysisUseCase)

    // Presentation
    viewModelOf(::LoginViewModel)
    viewModelOf(::DashboardViewModel)
}




