package com.neurogenesis.data_core.di

import com.neurogenesis.data_core.network.KtorNeoService
import org.koin.dsl.module

val networkModule = module {
    single { KtorNeoService() }
}