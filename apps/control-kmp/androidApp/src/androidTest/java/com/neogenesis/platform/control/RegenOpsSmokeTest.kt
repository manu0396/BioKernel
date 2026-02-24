package com.neogenesis.platform.control

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neogenesis.platform.control.android.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegenOpsSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginListProtocolsStartRunShowsEvent() {
        composeRule.onNodeWithText("Start login").assertExists().performClick()
        composeRule.onNodeWithText("I've authorized").assertExists().performClick()
        composeRule.onNodeWithText("Run Control").assertExists().performClick()
        composeRule.onNodeWithText("Start").assertExists().performClick()
        composeRule.onNodeWithText("RUN_STARTED", substring = true).assertExists()
    }
}
