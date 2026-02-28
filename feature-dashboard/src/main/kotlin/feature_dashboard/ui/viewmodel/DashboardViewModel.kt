package feature_dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.ToxicityLevel
import com.neogenesis.domain.usecases.SyncRetinaDataUseCase
import com.neogenesis.session.manager.SessionManager
import feature_dashboard.contract.DashboardIntent
import feature_dashboard.contract.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DashboardViewModel(
    private val syncRetinaDataUseCase: SyncRetinaDataUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Backward compatible ISO 8601 parser for API 24+
    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Formatter for UI display
    private val displayFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    init {
        handleIntent(DashboardIntent.LoadRetinaData)
    }

    /**
     * MVI Intent Handler - Single entry point for UI actions
     */
    fun handleIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadRetinaData -> fetchDashboardData()
            is DashboardIntent.InitializeAuthorizedSession -> {
                fetchDashboardData()
            }
            is DashboardIntent.FilterByToxicity -> filterBy(intent.level)
            is DashboardIntent.SelectRecord -> _state.update {
                it.copy(selectedRecord = intent.record)
            }
            is DashboardIntent.DismissDialog -> _state.update {
                it.copy(selectedRecord = null)
            }
        }
    }

    fun filterBy(level: ToxicityLevel?) {
        _state.update { currentState ->
            val filtered = if (level == null) {
                currentState.allSamples
            } else {
                currentState.allSamples.filter { it.toxicity == level }
            }
            currentState.copy(results = filtered)
        }
    }

    private fun fetchDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val patientId = sessionManager.getUserId() ?: "BIO-USER-DEMO-001"

            syncRetinaDataUseCase(patientId)
                .onSuccess { samples ->
                    val analyses = samples.map { sample ->
                        RetinaAnalysis(
                            id = sample.id,
                            rawHash = "0x${sample.id.hashCode().toString(16).uppercase()}",
                            countryIso = "GLO",
                            compatibilityScore = (1.0 - sample.toxicityScore) * 100,
                            toxicity = mapScoreToToxicity(sample.toxicityScore),
                            toxicityScore = sample.toxicityScore.toFloat(),
                            timestamp = parseIsoToLong(sample.date),
                            date = formatIsoToDisplay(sample.date),
                            notes = "BioKernel Cloud Sync: Success"
                        )
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            allSamples = analyses,
                            results = analyses,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
        }
    }

    // --- Helper Mappings ---

    private fun mapScoreToToxicity(score: Double): ToxicityLevel {
        return when {
            score < 0.2 -> ToxicityLevel.LOW
            score < 0.5 -> ToxicityLevel.MODERATE
            score < 0.8 -> ToxicityLevel.HIGH
            else -> ToxicityLevel.LETHAL
        }
    }

    private fun parseIsoToLong(isoString: String): Long {
        return try {
            // Remove 'Z' or timezone offset for SimpleDateFormat
            val cleanDate = isoString.substringBefore(".")
                .substringBefore("Z")
                .substringBefore("+")
            isoParser.parse(cleanDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun formatIsoToDisplay(isoString: String): String {
        return try {
            val cleanDate = isoString.substringBefore(".")
                .substringBefore("Z")
                .substringBefore("+")
            val date = isoParser.parse(cleanDate)
            if (date != null) displayFormatter.format(date) else isoString
        } catch (e: Exception) {
            isoString
        }
    }
}