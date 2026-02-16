package feature_dashboard.di


import feature_dashboard.ui.viewmodel.DashboardViewModel
import feature_dashboard.ui.viewmodel.RetinaDashboardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dashboardModule = module {
    viewModelOf(::RetinaDashboardViewModel)
    viewModelOf(::DashboardViewModel)
}



