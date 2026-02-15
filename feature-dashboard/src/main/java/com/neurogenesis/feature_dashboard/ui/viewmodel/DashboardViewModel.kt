package com.neurogenesis.feature_dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neurogenesis.domain.usecases.SyncRetinaAnalysisUseCase
import com.neurogenesis.feature_dashboard.contract.DashboardIntent
import com.neurogenesis.feature_dashboard.contract.DashboardViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getRetinaAnalysisUseCase: GetRetinaAnalysisUseCase,
    private val syncRetinaAnalysisUseCase: SyncRetinaAnalysisUseCase // New Dependency
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardViewState())
    val state = _state.asStateFlow()

    init {
        handleIntent(DashboardIntent.LoadRetinaData)
    }

    fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadRetinaData -> loadScientificData()
        }
    }

    private fun loadScientificData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // 1. Subscribe to the Flow (Source of Truth)
            // We launch this separately so it keeps collecting even if sync fails
            launch {
                getRetinaAnalysisUseCase()
                    .collect { data ->
                        _state.update { it.copy(results = data) }
                    }
            }

            // 2. Perform the Sync
            try {
                // Trigger the repository to fetch new data from "Cloud"
                syncRetinaAnalysisUseCase("SCIENTIST-ALPHA-01")
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}