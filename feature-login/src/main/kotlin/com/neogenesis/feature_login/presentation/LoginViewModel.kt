package com.neogenesis.feature_login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neogenesis.domain.auth.LoginEffect
import com.neogenesis.domain.auth.LoginIntent
import com.neogenesis.domain.auth.LoginViewState
import com.neogenesis.domain.model.SessionMetadata
import com.neogenesis.domain.repository.LoginRepository
import com.neogenesis.session.manager.SessionManager // INYECCIÓN NECESARIA
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginViewModel(
    private val loginRepository: LoginRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginViewState())
    val state: StateFlow<LoginViewState> = _state.asStateFlow()

    private val _effect = Channel<LoginEffect>()
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateUser -> _state.update { it.copy(user = intent.user) }
            is LoginIntent.SubmitLogin -> login(intent.user, intent.pass)
        }
    }

    private fun login(user: String, pass: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = loginRepository.login(user, pass)

            result.onSuccess { response ->
                sessionManager.saveSession(
                    token = "mock_2026",
                    userId = "BIO-USER-DEMO-001"
                )

                val validMetadata = SessionMetadata(
                    id = System.currentTimeMillis(),
                    patientId = "BIO-USER-DEMO-001",
                    lastSync = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        isAuthorized = true,
                        sessionMetadata = validMetadata
                    )
                }

                _effect.send(LoginEffect.NavigateToRetinaDashboard)
            }

            result.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, error = error.message ?: "Unknown Login Error")
                }
                _effect.send(LoginEffect.ShowToast("Error: ${error.message}"))
            }
        }
    }
}