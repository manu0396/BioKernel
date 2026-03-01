package com.neogenesis.platform.control.presentation

import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.control.data.remote.SimulatorApi
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NoOpLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegenOpsViewModelTest {
    @Test
    fun startRunNavigatesToLive() = runTest {
        val protocol = sampleProtocol()
        val protocolFlow = MutableStateFlow(listOf(protocol))
        val runsFlow = MutableStateFlow<List<Run>>(emptyList())
        val repository = mockRepository(protocolFlow, runsFlow)
        val simulatorApi = mockk<SimulatorApi>(relaxed = true)
        val viewModel =
            RegenOpsViewModel(
                config = sampleConfig(),
                repository = repository,
                oidcRepository = mockOidc(),
                commercialApi = mockk(relaxed = true),
                exportsApi = mockk(relaxed = true),
                traceApi = mockk(relaxed = true),
                simulatorApi = simulatorApi,
                logger = NoOpLogger
            )

        val expectedRun = sampleRun(protocol)
        coEvery { repository.startRun(protocol.id.value, protocol.versions.first().id.value) } returns
            ApiResult.Success(expectedRun)

        withTimeout(1_000) { viewModel.state.first { it.selectedProtocol != null } }

        viewModel.startRun()
        assertTrue(viewModel.state.value.isStartingRun)

        val liveState = withTimeout(2_000) { viewModel.state.first { it.screen == AppScreen.LIVE_RUN } }
        assertEquals(expectedRun.id.value, liveState.selectedRunId)
        assertFalse(liveState.isStartingRun)
    }

    @Test
    fun startSimulatedRunNavigatesToLive() = runTest {
        val protocol = sampleProtocol()
        val protocolFlow = MutableStateFlow(listOf(protocol))
        val runsFlow = MutableStateFlow<List<Run>>(emptyList())
        val repository = mockRepository(protocolFlow, runsFlow)
        val simulatorApi = mockk<SimulatorApi>()
        val viewModel =
            RegenOpsViewModel(
                config = sampleConfig(),
                repository = repository,
                oidcRepository = mockOidc(),
                commercialApi = mockk(relaxed = true),
                exportsApi = mockk(relaxed = true),
                traceApi = mockk(relaxed = true),
                simulatorApi = simulatorApi,
                logger = NoOpLogger
            )

        coEvery { simulatorApi.startSimulatedRun(protocol.id.value, any()) } returns ApiResult.Success("run-sim-1")

        withTimeout(1_000) { viewModel.state.first { it.selectedProtocol != null } }

        viewModel.startSimulatedRun(SimulationConfig(durationMinutes = 1, tickMillis = 250))
        assertTrue(viewModel.state.value.isStartingRun)

        val liveState = withTimeout(2_000) { viewModel.state.first { it.screen == AppScreen.LIVE_RUN } }
        assertEquals("run-sim-1", liveState.selectedRunId)
        assertTrue(liveState.simulatedRunEnabled)
        assertFalse(liveState.isStartingRun)
    }

    private fun sampleConfig() = AppConfig(
        httpBaseUrl = "http://localhost",
        grpcHost = "localhost",
        grpcPort = 9090,
        grpcUseTls = false,
        oidcIssuer = "",
        oidcClientId = "",
        oidcAudience = null,
        tenantId = "tenant-1",
        traceModeEnabled = false,
        demoModeEnabled = false,
        founderModeEnabled = false,
        commercialModeEnabled = false
    )

    private fun mockRepository(
        protocolFlow: Flow<List<Protocol>>,
        runsFlow: Flow<List<Run>>
    ): RegenOpsRepository {
        val repository = mockk<RegenOpsRepository>()
        every { repository.protocols } returns protocolFlow
        every { repository.runs } returns runsFlow
        every { repository.streamEvents(any()) } returns emptyFlow()
        every { repository.streamTelemetry(any()) } returns emptyFlow()
        return repository
    }

    private fun mockOidc(): OidcRepository {
        val oidc = mockk<OidcRepository>()
        every { oidc.hasTokens() } returns false
        return oidc
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

    private fun sampleRun(protocol: Protocol): Run =
        Run(
            id = RunId("run-123"),
            protocolId = protocol.id,
            protocolVersionId = protocol.versions.first().id,
            status = RunStatus.RUNNING,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = null
        )
}
