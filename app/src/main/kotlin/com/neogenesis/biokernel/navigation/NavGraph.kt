// app/src/main/kotlin/com/neogenesis/biokernel/navigation/NavGraph.kt
package com.neogenesis.biokernel.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neogenesis.feature_login.ui.LoginScreen
import feature_dashboard.ui.screens.DashboardScreen
import org.koin.androidx.compose.koinViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
}

@Composable
fun BioKernelNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = koinViewModel(),
                onAuthenticated = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel = koinViewModel())
        }
    }
}