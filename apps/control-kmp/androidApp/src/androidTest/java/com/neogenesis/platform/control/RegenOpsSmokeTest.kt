package com.neogenesis.platform.control

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.neogenesis.platform.control.presentation.AuthScreen
import com.neogenesis.platform.control.presentation.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RegenOpsSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun missingIdentityConfigurationRemainsExplicitlyUnauthenticated() {
        composeRule.setContent {
            AuthScreen(
                state = AuthUiState(statusMessage = "OIDC config missing"),
                onStart = {},
                onOpen = {},
                onPoll = {},
            )
        }

        assertEquals(1, composeRule.onAllNodesWithText("Configuration Error").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Enter Demo Session").fetchSemanticsNodes().size)
    }
}
