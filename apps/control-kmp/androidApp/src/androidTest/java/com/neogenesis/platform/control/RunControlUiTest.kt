package com.neogenesis.platform.control

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neogenesis.platform.control.presentation.RunControlScreen
import com.neogenesis.platform.control.presentation.SimulationConfig
import com.neogenesis.platform.control.presentation.design.NgStatus
import com.neogenesis.platform.control.presentation.design.NgStatusChip
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertTrue(composeRule.onAllNodesWithText("v1", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun demoModeStartsOnlyTheLabeledDemoJourney() {
        val protocol = sampleProtocol()
        var demoRequests = 0
        var connectedRequests = 0
        composeRule.setContent {
            RunControlScreen(
                protocols = listOf(protocol),
                selectedProtocol = protocol,
                selectedVersion = protocol.versions.first(),
                runs = emptyList(),
                demoModeEnabled = true,
                simulatedRunEnabled = false,
                isStartingRun = false,
                onSimulatedRunToggle = {},
                onSelectProtocol = {},
                onSelectVersion = {},
                onStartRun = { connectedRequests += 1 },
                onStartSimulatedRun = {},
                onStartDemoRun = { demoRequests += 1 },
                onPauseRun = {},
                onAbortRun = {},
                onSelectRun = {},
                onRefreshRuns = {},
            )
        }

        composeRule.onNodeWithText("Start Demo Run").assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(1, demoRequests)
            assertEquals(0, connectedRequests)
        }
        composeRule.onNodeWithText("Start Mission").assertDoesNotExist()
    }

    @Test
    fun startMissionInvokesConnectedRunHandler() {
        val protocol = sampleProtocol()
        var startRequests = 0
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
                onStartRun = { startRequests += 1 },
                onStartSimulatedRun = {},
                onStartDemoRun = {},
                onPauseRun = {},
                onAbortRun = {},
                onSelectRun = {},
                onRefreshRuns = {},
            )
        }

        composeRule.onNodeWithText("Start Mission").performClick()
        composeRule.runOnIdle { assertEquals(1, startRequests) }
    }

    @Test
    fun simulationSubmitsItsConfigurationToTheHandler() {
        val protocol = sampleProtocol()
        var submittedConfig: SimulationConfig? = null
        composeRule.setContent {
            var simulated by remember { mutableStateOf(false) }
            RunControlScreen(
                protocols = listOf(protocol),
                selectedProtocol = protocol,
                selectedVersion = protocol.versions.first(),
                runs = emptyList(),
                demoModeEnabled = false,
                simulatedRunEnabled = simulated,
                isStartingRun = false,
                onSimulatedRunToggle = { simulated = it },
                onSelectProtocol = {},
                onSelectVersion = {},
                onStartRun = {},
                onStartSimulatedRun = { submittedConfig = it },
                onStartDemoRun = {},
                onPauseRun = {},
                onAbortRun = {},
                onSelectRun = {},
                onRefreshRuns = {},
            )
        }

        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Start Simulation").performClick()
        composeRule.onNodeWithText("Start").performClick()
        composeRule.runOnIdle {
            assertEquals(30, submittedConfig?.durationMinutes)
            assertEquals(250, submittedConfig?.tickMillis)
        }
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
