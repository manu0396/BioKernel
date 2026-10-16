package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.control.presentation.SimulationConfig
import com.neogenesis.platform.shared.network.ApiResult
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DemoSimulatorApiTest {
    @Test
    fun createsDistinctExplicitlySimulatedRunIds() = runTest {
        val api = DemoSimulatorApi()

        val first = api.startSimulatedRun("protocol-1", SimulationConfig())
        val second = api.startSimulatedRun("protocol-1", SimulationConfig())

        assertTrue(first is ApiResult.Success)
        assertTrue(second is ApiResult.Success)
        assertTrue(first.value.startsWith("run-simulated-demo-"))
        assertTrue(second.value.startsWith("run-simulated-demo-"))
        assertNotEquals(first.value, second.value)
    }
}
