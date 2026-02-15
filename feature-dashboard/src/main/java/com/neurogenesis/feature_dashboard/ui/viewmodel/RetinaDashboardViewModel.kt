package com.neurogenesis.feature_dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurogenesis.domain.model.RetinaAnalysis
import com.neurogenesis.domain.usecases.GetRetinaAnalysisUseCase
import com.neurogenesis.domain.usecases.SyncRetinaAnalysisUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RetinaDashboardViewModel(
    private val getAnalysis: GetRetinaAnalysisUseCase,
    private val syncAnalysis: SyncRetinaAnalysisUseCase // New Dependency
) : ViewModel() {

    private val _state = MutableStateFlow<List<RetinaAnalysis>>(emptyList())
    val state: StateFlow<List<RetinaAnalysis>> = _state

    init {
        // 1. Start observing Local Data immediately
        viewModelScope.launch {
            getAnalysis().collect { _state.value = it }
        }

        // 2. Trigger Cloud Sync (Side Effect)
        viewModelScope.launch {
            try {
                // In a real app, pass the actual ID from Session/Auth
                syncAnalysis("SCIENTIST-ALPHA-01")
            } catch (e: Exception) {
                // Handle sync error (e.g., show snackbar)
                e.printStackTrace()
            }
        }
    }
}