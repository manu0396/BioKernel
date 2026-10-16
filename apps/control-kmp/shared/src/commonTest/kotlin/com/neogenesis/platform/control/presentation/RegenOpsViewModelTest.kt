package com.neogenesis.platform.control.presentation

import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.control.data.remote.DevicePolicyApi
import com.neogenesis.platform.control.data.remote.SimulatorApi
import com.neogenesis.platform.control.device.DeviceInfoStore
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.domain.device.DeviceClass
import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DeviceTier
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NoOpLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.mockk.coVerify

class RegenOpsViewModelTest {
    @Test
    fun demoEntryRemainsUnauthenticatedAndBlocksConnectedRuns() = runTest {
        val protocol = sampleProtocol()
        val repository = mockRepository(
            MutableStateFlow(listOf(protocol)),
            MutableStateFlow(emptyList())
        )
        coEvery { repository.refreshProtocols() } returns ApiResult.Success(listOf(protocol))
        coEvery { repository.refreshRuns() } returns ApiResult.Success(emptyList())
        val viewModel = RegenOpsViewModel(
            config = sampleConfig(demoModeEnabled = true),
            repository = repository,
            oidcRepository = mockOidc(),
            devicePolicyApi = mockk(relaxed = true),
            deviceInfoStore = sampleDeviceInfoStore(),
            commercialApi = mockk(relaxed = true),
            exportsApi = mockk(relaxed = true),
            traceApi = mockk(relaxed = true),
            simulatorApi = mockk(relaxed = true),
            logger = NoOpLogger
        )

        try {
            viewModel.enterDemoSession()
            assertTrue(viewModel.state.value.auth.isDemoSession)
            assertFalse(viewModel.state.value.auth.isAuthenticated)
            assertEquals(AppScreen.PROTOCOLS, viewModel.state.value.screen)

            viewModel.startRun()

            assertEquals("Connected-device runs are unavailable in demo mode.", viewModel.state.value.errorBanner)
            coVerify(exactly = 0) { repository.startRun(any(), any()) }
        } finally {
            viewModel.close()
        }
    }

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
                devicePolicyApi = mockk(relaxed = true),
                deviceInfoStore = sampleDeviceInfoStore(),
                commercialApi = mockk(relaxed = true),
                exportsApi = mockk(relaxed = true),
                traceApi = mockk(relaxed = true),
                simulatorApi = simulatorApi,
                logger = NoOpLogger
            )

        try {
            val expectedRun = sampleRun(protocol)
            val startRunGate = CompletableDeferred<Unit>()
            coEvery { repository.startRun(protocol.id.value, protocol.versions.first().id.value) } coAnswers {
                startRunGate.await()
                ApiResult.Success(expectedRun)
            }

            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { viewModel.state.first { it.selectedProtocol != null } }
            }

            viewModel.startRun()
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { viewModel.state.first { it.isStartingRun } }
            }
            assertTrue(viewModel.state.value.isStartingRun)

            startRunGate.complete(Unit)

            val liveState = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { viewModel.state.first { it.screen == AppScreen.LIVE_RUN && !it.isStartingRun } }
            }
            assertEquals(expectedRun.id.value, liveState.selectedRunId)
            assertFalse(liveState.isStartingRun)
        } finally {
            viewModel.close()
        }
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
                devicePolicyApi = mockk(relaxed = true),
                deviceInfoStore = sampleDeviceInfoStore(),
                commercialApi = mockk(relaxed = true),
                exportsApi = mockk(relaxed = true),
                traceApi = mockk(relaxed = true),
                simulatorApi = simulatorApi,
                logger = NoOpLogger
            )

        try {
            val simGate = CompletableDeferred<Unit>()
            coEvery { simulatorApi.startSimulatedRun(protocol.id.value, any()) } coAnswers {
                simGate.await()
                ApiResult.Success("run-sim-1")
            }

            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { viewModel.state.first { it.selectedProtocol != null } }
            }

            viewModel.startSimulatedRun(SimulationConfig(durationMinutes = 1, tickMillis = 250))
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { viewModel.state.first { it.isStartingRun } }
            }
            assertTrue(viewModel.state.value.isStartingRun)

            simGate.complete(Unit)

            val liveState = withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(5_000) { viewModel.state.first { it.screen == AppScreen.LIVE_RUN && !it.isStartingRun } }
            }
            assertEquals("run-sim-1", liveState.selectedRunId)
            assertTrue(liveState.simulatedRunEnabled)
            assertFalse(liveState.isStartingRun)
        } finally {
            viewModel.close()
        }
    }

    private fun sampleConfig(demoModeEnabled: Boolean = false) = AppConfig(
        httpBaseUrl = "http://localhost",
        grpcHost = "localhost",
        grpcPort = 9090,
        grpcUseTls = false,
        appVersion = "1.0.0",
        oidcIssuer = "",
        oidcClientId = "",
        oidcAudience = null,
        tenantId = "tenant-1",
        traceModeEnabled = false,
        demoModeEnabled = demoModeEnabled,
        founderModeEnabled = false,
        commercialModeEnabled = false
    )

    private fun sampleDeviceInfoStore(): DeviceInfoStore =
        DeviceInfoStore(
            DeviceInfo(
                deviceId = "device-1",
                deviceClass = DeviceClass.WINDOWS_DESKTOP,
                tier = DeviceTier.TIER_1,
                appVersion = "1.0.0",
                platform = "desktop",
                model = "test",
                osVersion = "1",
                policyVersion = null
            )
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
        every { repository.close() } returns Unit
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
