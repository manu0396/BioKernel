package com.neogenesis.feature_login.di

import com.neogenesis.feature_login.presentation.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loginModule = module {
    viewModel {
        LoginViewModel(
            loginRepository = get(),
            sessionManager = get()
        )
    }
}






