package com.neogenesis.platform.control

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neogenesis.platform.control.presentation.AppScreen
import com.neogenesis.platform.control.presentation.LiveRunScreen
import com.neogenesis.platform.control.presentation.RunControlScreen
import com.neogenesis.platform.control.presentation.SimulationConfig
import com.neogenesis.platform.control.presentation.design.NgStatus
import com.neogenesis.platform.control.presentation.design.NgStatusChip
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunControlUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun simulateChipToggles() {
        composeRule.setContent {
            var enabled by remember { mutableStateOf(false) }
            Row {
                if (enabled) {
                    NgStatusChip(
                        text = "SIMULATION",
                        status = NgStatus.Warning,
                        onClick = { enabled = false }
                    )
                } else {
                    NgStatusChip(
                        text = "SIMULATE",
                        status = NgStatus.Warning,
                        onClick = { enabled = true }
                    )
                }
            }
            if (enabled) {
                androidx.compose.material3.Text("SIMULATION_ON", style = MaterialTheme.typography.bodySmall)
            } else {
                androidx.compose.material3.Text("SIMULATION_OFF", style = MaterialTheme.typography.bodySmall)
            }
        }

        composeRule.onNodeWithText("SIMULATE").assertExists().performClick()
        composeRule.onNodeWithText("SIMULATION").assertExists()
        composeRule.onNodeWithText("SIMULATION_ON").assertExists()
    }

    @Test
    fun versionPickerShowsListAndClose() {
        val protocol = sampleProtocol()
        composeRule.setContent {
            RunControlScreen(
                protocols = listOf(protocol),
                selectedProtocol = protocol,
                selectedVersion = protocol.versions.first(),
                runs = emptyList(),
                demoModeEnabled = false,
                simulatedRunEnabled = false,
                isStartingRun = false,
                onSimulatedRunToggle = {},
                onSelectProtocol = {},
                onSelectVersion = {},
                onStartRun = {},
                onStartSimulatedRun = {},
                onStartDemoRun = {},
                onPauseRun = {},
                onAbortRun = {},
                onSelectRun = {},
                onRefreshRuns = {},
            )
        }

        composeRule.onAllNodesWithText("Change")[1].performClick()
        composeRule.onNodeWithText("Select Version").assertExists()
        composeRule.onNodeWithText("Close").assertExists()
        composeRule.onNodeWithText("v1", substring = true).assertExists()
    }

    @Test
    fun startMissionShowsLoadingThenNavigates() {
        composeRule.mainClock.autoAdvance = false
        val protocol = sampleProtocol()
        composeRule.setContent {
            var screen by remember { mutableStateOf(AppScreen.RUN_CONTROL) }
            var isStarting by remember { mutableStateOf(false) }
            var startRequests by remember { mutableStateOf(0) }

            LaunchedEffect(startRequests) {
                if (startRequests > 0) {
                    isStarting = true
                    delay(300)
                    screen = AppScreen.LIVE_RUN
                    isStarting = false
                }
            }

            if (screen == AppScreen.RUN_CONTROL) {
                RunControlScreen(
                    protocols = listOf(protocol),
                    selectedProtocol = protocol,
                    selectedVersion = protocol.versions.first(),
                    runs = emptyList(),
                    demoModeEnabled = false,
                    simulatedRunEnabled = false,
                    isStartingRun = isStarting,
                    onSimulatedRunToggle = {},
                    onSelectProtocol = {},
                    onSelectVersion = {},
                    onStartRun = { startRequests += 1 },
                    onStartSimulatedRun = {},
                    onStartDemoRun = {},
                    onPauseRun = {},
                    onAbortRun = {},
                    onSelectRun = {},
                    onRefreshRuns = {},
                )
            } else {
                LiveRunScreen(
                    runId = "run-123",
                    runEvents = emptyList(),
                    telemetryFrames = emptyList(),
                )
            }
        }

        composeRule.onNodeWithText("Start Mission").assertExists().performClick()
        composeRule.onNodeWithText("Start Mission").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(350)
        composeRule.onNodeWithText("Mission Live Control").assertExists()
    }

    @Test
    fun startSimulationShowsLoadingThenNavigates() {
        composeRule.mainClock.autoAdvance = false
        val protocol = sampleProtocol()
        composeRule.setContent {
            var screen by remember { mutableStateOf(AppScreen.RUN_CONTROL) }
            var isStarting by remember { mutableStateOf(false) }
            var simulated by remember { mutableStateOf(false) }
            var simRequests by remember { mutableStateOf(0) }

            LaunchedEffect(simRequests) {
                if (simRequests > 0) {
                    isStarting = true
                    delay(300)
                    screen = AppScreen.LIVE_RUN
                    isStarting = false
                }
            }

            if (screen == AppScreen.RUN_CONTROL) {
                RunControlScreen(
                    protocols = listOf(protocol),
                    selectedProtocol = protocol,
                    selectedVersion = protocol.versions.first(),
                    runs = emptyList(),
                    demoModeEnabled = false,
                    simulatedRunEnabled = simulated,
                    isStartingRun = isStarting,
                    onSimulatedRunToggle = { simulated = it },
                    onSelectProtocol = {},
                    onSelectVersion = {},
                    onStartRun = {},
                    onStartSimulatedRun = { _: SimulationConfig -> simRequests += 1 },
                    onStartDemoRun = {},
                    onPauseRun = {},
                    onAbortRun = {},
                    onSelectRun = {},
                    onRefreshRuns = {},
                )
            } else {
                LiveRunScreen(
                    runId = "run-456",
                    runEvents = emptyList(),
                    telemetryFrames = emptyList(),
                )
            }
        }

        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Start Simulation").assertExists().performClick()
        composeRule.onNodeWithText("Start").assertExists().performClick()
        composeRule.onNodeWithText("Start Simulation").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(350)
        composeRule.onNodeWithText("Mission Live Control").assertExists()
    }

    private fun sampleProtocol(): Protocol {
        val protocolId = ProtocolId("proto-1")
        val version = ProtocolVersion(
            id = ProtocolVersionId("proto-1-v1"),
            protocolId = protocolId,
            version = "1",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            author = "system",
            payload = "{}",
            published = true
        )
        return Protocol(
            id = protocolId,
            name = "Test Protocol",
            summary = "Test",
            latestVersion = version,
            versions = listOf(version)
        )
    }
}

