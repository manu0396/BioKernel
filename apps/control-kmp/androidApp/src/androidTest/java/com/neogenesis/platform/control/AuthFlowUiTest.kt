package com.neogenesis.platform.control

import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neogenesis.platform.control.android.BuildConfig
import com.neogenesis.platform.control.android.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowUiTest {
    private companion object {
        const val UI_TIMEOUT_MS = 30_000L
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun demoSessionRemainsUnauthenticatedAndSupportsSyntheticRuns() {
        assumeTrue(BuildConfig.DEMO_MODE)
        composeRule.onNodeWithText("Enter Demo Session").assertExists().performClick()
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            composeRule.onAllNodesWithText("Demo Protocol").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("DEMO SESSION - NOT AUTHENTICATED").assertExists()
        composeRule.onNodeWithText("Control").assertExists().performClick()
        composeRule.onNodeWithText("Start Demo Run").assertExists().performClick()
        composeRule.onNodeWithText("DEMO RUN").assertExists()
        composeRule.onNodeWithText("run-demo").assertExists()

        composeRule.onNodeWithText("Control").assertExists().performClick()
        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Start Simulation").assertExists().performClick()
        composeRule.onNodeWithText("Start").assertExists().performClick()
        composeRule.waitUntil(UI_TIMEOUT_MS) {
            composeRule.onAllNodesWithText("run-simulated-demo-1").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(2, composeRule.onAllNodesWithText("SIMULATION").fetchSemanticsNodes().size)
    }
}
