package com.neogenesis.domain.auth

import com.neogenesis.domain.model.SessionMetadata

data class LoginViewState(
    val isLoading: Boolean = false,
    val user: String = "",
    val error: String? = null,
    val isAuthorized: Boolean = false,
    val sessionMetadata: SessionMetadata? = null
)

data class LoginUiState(
    val user: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LoginIntent {
    data class UpdateUser(val user: String) : LoginIntent()
    data class SubmitLogin(val user: String, val pass: String) : LoginIntent()
}

sealed class LoginEffect {
    object NavigateToRetinaDashboard : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
    data object NavigateToDashboard : LoginEffect()
}




