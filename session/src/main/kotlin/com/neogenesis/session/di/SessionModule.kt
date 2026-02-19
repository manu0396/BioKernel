package com.neogenesis.session.di

import com.neogenesis.domain.session.SessionManager
import com.neogenesis.session.manager.SessionManagerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sessionModule = module {
    single<SessionManager> { SessionManagerImpl(androidContext(), get()) }
}


