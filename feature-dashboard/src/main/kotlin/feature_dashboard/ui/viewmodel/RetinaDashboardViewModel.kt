package feature_dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neogenesis.domain.usecases.SyncRetinaAnalysisUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RetinaDashboardViewModel(
    private val getAnalysis: GetRetinaAnalysisUseCase,
    private val syncAnalysis: SyncRetinaAnalysisUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<List<RetinaAnalysis>>(emptyList())
    val state: StateFlow<List<RetinaAnalysis>> = _state

    init {
        viewModelScope.launch {
            getAnalysis().collect { _state.value = it }
        }

        viewModelScope.launch {
            try {
                syncAnalysis("SCIENTIST-ALPHA-01")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}



