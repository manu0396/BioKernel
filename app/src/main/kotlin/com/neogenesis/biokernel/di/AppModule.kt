package com.neogenesis.biokernel.di

import androidx.fragment.app.FragmentActivity
import com.neogenesis.data_core.persistence.BiometricAuthManagerImpl
import com.neogenesis.data.repository.RetinaRepositoryImpl
import com.neogenesis.data_core.persistence.BiometricAuthManager
import com.neogenesis.domain.repository.RetinaRepository
import com.neogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.LoginUseCase
import feature_dashboard.ui.viewmodel.DashboardViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<RetinaRepository> { RetinaRepositoryImpl(get(), get()) }
    factoryOf(::LoginUseCase)
    factoryOf(::GetRetinaAnalysisUseCase)
    viewModelOf(::DashboardViewModel)
    factory<BiometricAuthManager> { (activity: FragmentActivity) ->
        BiometricAuthManagerImpl(
            activity = activity
        )
    }
}







