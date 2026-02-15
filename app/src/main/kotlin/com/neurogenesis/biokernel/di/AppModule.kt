package com.neurogenesis.biokernel.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.neurogenesis.data.db.BioKernelDatabase
import com.neurogenesis.data.repository.RetinaRepositoryImpl
import com.neurogenesis.data_core.network.KtorNeoService
import com.neurogenesis.domain.repository.RetinaRepository
import com.neurogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neurogenesis.domain.usecases.LoginUseCase
import com.neurogenesis.feature_dashboard.ui.viewmodel.DashboardViewModel
import com.neurogenesis.feature_login.presentation.LoginViewModel
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

    // Data & Domain (Using Koin 4.x singleOf/factoryOf)
    singleOf(::KtorNeoService)
    // Bind the Implementation from :data to the Interface from :domain
    single<RetinaRepository> { RetinaRepositoryImpl(get(), get()) }

    factoryOf(::LoginUseCase)
    factoryOf(::GetRetinaAnalysisUseCase)

    // Presentation
    viewModelOf(::LoginViewModel)
    viewModelOf(::DashboardViewModel)
}