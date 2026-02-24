package com.neogenesis.domain.di

import com.neogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.LoginUseCase
import com.neogenesis.domain.usecases.SyncRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.SyncRetinaDataUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf(::GetRetinaAnalysisUseCase)
    factoryOf(::SyncRetinaAnalysisUseCase)
    factoryOf(::LoginUseCase)
    factoryOf(::SyncRetinaDataUseCase)
}






