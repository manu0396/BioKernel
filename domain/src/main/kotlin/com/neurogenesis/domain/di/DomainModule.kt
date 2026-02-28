package com.neurogenesis.domain.di

import com.neurogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neurogenesis.domain.usecases.LoginUseCase
import com.neurogenesis.domain.usecases.SyncRetinaAnalysisUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::GetRetinaAnalysisUseCase)
    factoryOf(::SyncRetinaAnalysisUseCase)
    factoryOf(::LoginUseCase)
}