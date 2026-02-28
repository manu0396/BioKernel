package com.neogenesis.feature_login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neogenesis.domain.auth.LoginEffect
import com.neogenesis.domain.auth.LoginIntent
import com.neogenesis.domain.auth.LoginViewState
import com.neogenesis.domain.usecases.LoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val loginUseCase: LoginUseCase) : ViewModel() {
    private val _state = MutableStateFlow(LoginViewState())
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect = _effect.asSharedFlow()

    fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateUser -> _state.update { it.copy(user = intent.user) }
            is LoginIntent.SubmitLogin -> performLogin(intent.user, intent.pass)
        }
    }

    private fun performLogin(user: String, pass: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                loginUseCase.invoke(user, pass)
                _effect.emit(LoginEffect.NavigateToRetinaDashboard)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage) }
                _effect.emit(LoginEffect.ShowToast(e.message ?: "Authentication Failed"))
            }
        }
    }
}



