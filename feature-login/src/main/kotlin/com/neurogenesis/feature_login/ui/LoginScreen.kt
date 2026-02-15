package com.neurogenesis.feature_login.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neurogenesis.domain.auth.LoginEffect
import com.neurogenesis.domain.auth.LoginIntent
import com.neurogenesis.feature_login.R
import com.neurogenesis.feature_login.presentation.LoginViewModel

@Composable
fun LoginScreen(viewModel: LoginViewModel, onAuthenticated: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is LoginEffect.NavigateToRetinaDashboard) onAuthenticated()
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)) {
        Text("NeoGenesis BioKernel", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = state.user,
            onValueChange = { viewModel.handleIntent(LoginIntent.UpdateUser(it)) },
            label = { Text(stringResource(R.string.login_identifier_hint)) }
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.security_token)) }
        )

        Button(onClick = {
            viewModel.handleIntent(
                LoginIntent.SubmitLogin(
                    state.user,
                    password
                )
            )
        }) {
            if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.access_retina_dashboard))
        }
    }
}