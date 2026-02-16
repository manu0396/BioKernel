package com.neogenesis.session.di

import com.neogenesis.session.manager.SessionManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sessionModule = module {
    // Definimos el SessionManager como un Singleton
    single { SessionManager(androidContext()) }
}