package feature_dashboard.contract

import com.neogenesis.components.util.UiText
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.ToxicityLevel

data class DashboardState(
    val isLoading: Boolean = false,
    val allSamples: List<RetinaAnalysis> = emptyList(),
    val results: List<RetinaAnalysis> = emptyList(),
    val selectedRecord: RetinaAnalysis? = null,
    val error: UiText? = null
)

sealed class DashboardIntent {
    data object InitializeAuthorizedSession : DashboardIntent()
    object LoadRetinaData : DashboardIntent()
    data class SelectRecord(val record: RetinaAnalysis) : DashboardIntent()
    data object DismissDialog : DashboardIntent()
    data class FilterByToxicity(val level: ToxicityLevel?) : DashboardIntent()
}

sealed class DashboardEffect {
    data class ShowError(val message: String) : DashboardEffect()
}






