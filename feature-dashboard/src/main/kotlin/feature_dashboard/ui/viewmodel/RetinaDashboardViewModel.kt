package feature_dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.SyncRetinaAnalysisUseCase
import com.neogenesis.session.manager.SessionManager 
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RetinaDashboardViewModel(
    private val getAnalysis: GetRetinaAnalysisUseCase,
    private val syncAnalysis: SyncRetinaAnalysisUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow<List<RetinaAnalysis>>(emptyList())
    val state: StateFlow<List<RetinaAnalysis>> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val currentPatientId = sessionManager.getUserId() ?: "ANONYMOUS_USER"

        viewModelScope.launch {
            getAnalysis(currentPatientId).collect { list ->
                _state.value = list
            }
        }

        viewModelScope.launch {
            try {
                syncAnalysis(currentPatientId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


