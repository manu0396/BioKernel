package com.neogenesis.domain.auth

data class LoginViewState(
    val isLoading: Boolean = false,
    val user: String = "",
    val error: String? = null,
    val isAuthorized: Boolean = false
)

sealed class LoginIntent {
    data class UpdateUser(val user: String) : LoginIntent()
    data class SubmitLogin(val user: String, val pass: String) : LoginIntent()
}

sealed class LoginEffect {
    object NavigateToRetinaDashboard : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}



