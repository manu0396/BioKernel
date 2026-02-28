package com.neurogenesis.feature_dashboard.di

import com.neurogenesis.feature_dashboard.ui.viewmodel.DashboardViewModel
import com.neurogenesis.feature_dashboard.ui.viewmodel.RetinaDashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dashboardModule = module {
    viewModelOf(::RetinaDashboardViewModel)
    viewModelOf(::DashboardViewModel)
}