// feature_dashboard/ui/viewmodel/DashboardViewModel.kt
package feature_dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.SyncRetinaDataUseCase
import feature_dashboard.contract.DashboardIntent
import feature_dashboard.contract.DashboardUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getRetinaAnalysisUseCase: GetRetinaAnalysisUseCase,
    private val syncRetinaDataUseCase: SyncRetinaDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        handleIntent(DashboardIntent.LoadRetinaData)
    }

    fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadRetinaData -> loadRetinaData()
            is DashboardIntent.SelectRecord -> selectRecord(intent.record)
            is DashboardIntent.DismissDialog -> dismissDialog()
        }
    }

    private fun loadRetinaData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(2500)
            launch {
                getRetinaAnalysisUseCase()
                    .catch { e -> _uiState.update { it.copy(error = e.message) } }
                    .collect { list ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                results = list
                            )
                        }
                    }
            }

            try {
                syncRetinaDataUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Sync Failed: ${e.message}") }
            }
        }
    }

    private fun selectRecord(record: RetinaAnalysis) {
        _uiState.update { it.copy(selectedRecord = record) }
    }

    private fun dismissDialog() {
        _uiState.update { it.copy(selectedRecord = null) }
    }
}