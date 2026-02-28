package feature_dashboard.contract

import com.neogenesis.domain.model.RetinaAnalysis

data class DashboardUiState(
    val isLoading: Boolean = false,
    val results: List<RetinaAnalysis> = emptyList(),
    val selectedRecord: RetinaAnalysis? = null,
    val error: String? = null
)

sealed class DashboardIntent {
    object LoadRetinaData : DashboardIntent()
    data class SelectRecord(val record: RetinaAnalysis) : DashboardIntent()
    data object DismissDialog : DashboardIntent()
}

sealed class DashboardEffect {
    data class ShowError(val message: String) : DashboardEffect()
}



