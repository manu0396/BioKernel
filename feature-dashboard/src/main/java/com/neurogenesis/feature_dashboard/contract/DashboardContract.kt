package com.neurogenesis.feature_dashboard.contract

import com.neurogenesis.domain.model.RetinaAnalysis

data class DashboardViewState(
    val isLoading: Boolean = false,
    val results: List<RetinaAnalysis> = emptyList(),
    val error: String? = null
)

sealed class DashboardIntent {
    object LoadRetinaData : DashboardIntent()
}

sealed class DashboardEffect {
    data class ShowError(val message: String) : DashboardEffect()
}