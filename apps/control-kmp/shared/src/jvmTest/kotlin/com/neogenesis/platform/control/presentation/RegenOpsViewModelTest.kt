package com.neogenesis.platform.control.presentation

import app.cash.turbine.test
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.control.data.remote.CommercialApi
import com.neogenesis.platform.control.data.remote.ExportsApi
import com.neogenesis.platform.control.data.remote.SimulatorApi
import com.neogenesis.platform.control.data.remote.TraceApi
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.NoOpLogger
import com.neogenesis.platform.shared.telemetry.FlowRate
import com.neogenesis.platform.shared.telemetry.MPCPrediction
import com.neogenesis.platform.shared.telemetry.NozzleDisplacement
import com.neogenesis.platform.shared.telemetry.PIDState
import com.neogenesis.platform.shared.telemetry.PressureReading
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import com.neogenesis.platform.shared.telemetry.Temperature
import com.neogenesis.platform.shared.telemetry.ViscosityEstimation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RegenOpsViewModelTest {

    @Test
    fun startStreamingCollectsTelemetry() = runTest {
        val repository: RegenOpsRepository = mockk(relaxed = true)
        val oidcRepository: OidcRepository = mockk()
        val commercialApi: CommercialApi = mockk()
        val exportsApi: ExportsApi = mockk()
        val traceApi: TraceApi = mockk()

        val protocolsFlow = MutableStateFlow(emptyList<Protocol>())
        val runsFlow = MutableStateFlow(emptyList<Run>())
        val telemetryStream = MutableSharedFlow<TelemetryFrame>()

        every { repository.protocols } returns protocolsFlow
        every { repository.runs } returns runsFlow
        every { repository.streamTelemetry(any()) } returns telemetryStream
        every { repository.streamEvents(any()) } returns MutableSharedFlow()
        every { oidcRepository.hasTokens() } returns false

        val config = AppConfig(
            httpBaseUrl = "http://localhost:8080",
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

        val simulatorApi: SimulatorApi = mockk(relaxed = true)
        val logger: AppLogger = NoOpLogger

        val viewModel = RegenOpsViewModel(
            config = config,
            repository = repository,
            oidcRepository = oidcRepository,
            commercialApi = commercialApi,
            exportsApi = exportsApi,
            traceApi = traceApi,
            simulatorApi = simulatorApi,
            logger = logger
        )

        viewModel.startStreaming("run-123")
        val frame = TelemetryFrame(
            timestamp = Instant.parse("2024-01-01T00:00:00Z"),
            pressure = PressureReading(1.0),
            displacement = NozzleDisplacement(0.1),
            flowRate = FlowRate(0.2),
            temperature = Temperature(37.0),
            viscosity = ViscosityEstimation(0.3),
            pid = PIDState(0.1, 0.2, 0.3),
            mpc = MPCPrediction(1, 0.4)
        )

        viewModel.state.test {
            telemetryStream.emit(frame)
            val updated = awaitItem().let { if (it.telemetryFrames.contains(frame)) it else awaitItem() }
            assertEquals(true, updated.telemetryFrames.contains(frame))
            cancelAndIgnoreRemainingEvents()
        }
    }
