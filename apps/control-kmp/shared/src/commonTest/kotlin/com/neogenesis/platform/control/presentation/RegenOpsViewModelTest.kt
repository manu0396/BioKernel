package com.neogenesis.platform.control.presentation

import app.cash.turbine.test
import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.control.data.remote.CommercialApi
import com.neogenesis.platform.control.data.remote.ExportsApi
import com.neogenesis.platform.control.data.remote.TraceApi
import com.neogenesis.platform.control.domain.usecase.TelemetryExcelExporter
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.telemetry.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class RegenOpsViewModelTest {
    private val appConfig: AppConfig = mockk()
    private val repository: RegenOpsRepository = mockk()
    private val oidcRepository: OidcRepository = mockk()
    private val commercialApi: CommercialApi = mockk()
    private val exportsApi: ExportsApi = mockk()
    private val traceApi: TraceApi = mockk()
    private val telemetryExcelExporter: TelemetryExcelExporter = mockk()

    private lateinit var viewModel: RegenOpsViewModel

    private val protocolsFlow = MutableStateFlow<List<Protocol>>(emptyList())
    private val runsFlow = MutableStateFlow<List<Run>>(emptyList())
    private val telemetryStream = MutableSharedFlow<TelemetryFrame>()

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.protocols } returns protocolsFlow
        every { repository.runs } returns runsFlow
        every { oidcRepository.hasTokens() } returns false
        every { appConfig.commercialModeEnabled } returns false
        every { appConfig.founderModeEnabled } returns false
        every { appConfig.traceModeEnabled } returns false
        every { appConfig.demoModeEnabled } returns false

        viewModel =
            RegenOpsViewModel(
                appConfig,
                repository,
                oidcRepository,
                commercialApi,
                exportsApi,
                traceApi,
                telemetryExcelExporter,
            )
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
        // Reset the main dispatcher
    }

    @Test
    fun `startStreaming updates state and collects telemetry`() =
        runTest {
            val runId = "test-run-123"
            val initialTelemetryFrame =
                TelemetryFrame(
                    timestamp = Instant.parse("2024-01-01T00:00:00Z"),
                    pressure = PressureReading(0.0),
                    displacement = NozzleDisplacement(0.0),
                    flowRate = FlowRate(0.0),
                    temperature = Temperature(0.0),
                    viscosity = ViscosityEstimation(0.0),
                    pid = PIDState(0.0, 0.0, 0.0),
                    mpc = MPCPrediction(0, 0.0),
                )

            every { repository.streamEvents(any()) } returns MutableSharedFlow()
            every { repository.streamTelemetry(any()) } returns telemetryStream

            viewModel.state.test {
                // Initial state
                skipItems(1) // Skip initial state

                viewModel.startStreaming(runId)
                advanceUntilIdle()

                // State after startStreaming
                var emittedState = awaitItem()
                assertTrue(emittedState.isSimulationRunning)
                assertFalse(emittedState.isSimulationFinished)
                assertEquals("Connecting...", emittedState.streamStatus)
                assertTrue(emittedState.telemetryExportFrames.isEmpty())

                telemetryStream.emit(initialTelemetryFrame)
                advanceUntilIdle()

                // State after first telemetry frame
                emittedState = awaitItem()
                assertTrue(emittedState.telemetryFrames.contains(initialTelemetryFrame))
                assertTrue(emittedState.telemetryExportFrames.contains(initialTelemetryFrame))
                assertNull(emittedState.streamStatus)

                // Emit more frames to test MAX_TELEMETRY_EXPORT_FRAMES
                val frames =
                    (1..MAX_TELEMETRY_EXPORT_FRAMES + 10).map { i ->
                        TelemetryFrame(
                            timestamp = Instant.fromEpochMilliseconds(i.toLong()),
                            pressure = PressureReading(i.toDouble()),
                            displacement = NozzleDisplacement(i.toDouble()),
                            flowRate = FlowRate(i.toDouble()),
                            temperature = Temperature(i.toDouble()),
                            viscosity = ViscosityEstimation(i.toDouble()),
                            pid = PIDState(i.toDouble(), i.toDouble(), i.toDouble()),
                            mpc = MPCPrediction(i, i.toDouble()),
                        )
                    }
                frames.forEach { telemetryStream.emit(it) }
                advanceUntilIdle()

                emittedState = awaitItem() // State update after all frames emitted
                assertEquals(200, emittedState.telemetryFrames.size)
                assertEquals(MAX_TELEMETRY_EXPORT_FRAMES, emittedState.telemetryExportFrames.size)
                assertEquals(frames.last(), emittedState.telemetryExportFrames.last())
                assertEquals(frames[frames.size - MAX_TELEMETRY_EXPORT_FRAMES], emittedState.telemetryExportFrames.first())
            }
        }

    @Test
    fun `stopSimulation updates state correctly`() =
        runTest {
            val runId = "test-run-456"

            every { repository.streamEvents(any()) } returns MutableSharedFlow()
            every { repository.streamTelemetry(any()) } returns MutableSharedFlow()

            viewModel.state.test {
                skipItems(1) // Skip initial state

                viewModel.startStreaming(runId)
                advanceUntilIdle()
                awaitItem() // State update after startStreaming

                viewModel.stopSimulation()
                advanceUntilIdle()

                val emittedState = awaitItem()
                assertFalse(emittedState.isSimulationRunning)
                assertTrue(emittedState.isSimulationFinished)
            }
        }

    @Test
    fun `exportSimulationExcel generates and shares file`() =
        runTest {
            val runId = "export-run-789"
            val frames =
                listOf(
                    TelemetryFrame(
                        timestamp = Instant.parse("2024-01-01T00:00:01Z"),
                        pressure = PressureReading(1.0),
                        displacement = NozzleDisplacement(1.0),
                        flowRate = FlowRate(1.0),
                        temperature = Temperature(1.0),
                        viscosity = ViscosityEstimation(1.0),
                        pid = PIDState(1.0, 1.0, 1.0),
                        mpc = MPCPrediction(1, 1.0),
                    ),
                )
            val excelBytes = "excel_content".encodeToByteArray()
            var sharedBytes: ByteArray? = null
            var sharedFileName: String? = null
            var sharedMimeType: String? = null

            every { telemetryExcelExporter.toXlsxBytes(any(), any(), any()) } returns excelBytes

            val shareFile: (ByteArray, String, String) -> Unit = { bytes, fileName, mimeType ->
                sharedBytes = bytes
                sharedFileName = fileName
                sharedMimeType = mimeType
            }

            viewModel.state.test {
                skipItems(1) // Skip initial state

                // Set up a simulated run with telemetry for export
                viewModel.setSimulatedRunEnabled(true)
                viewModel.startStreaming(runId)
                frames.forEach { telemetryStream.emit(it) }
                advanceUntilIdle()
                awaitItem() // State after streaming starts
                awaitItem() // State after telemetry frames

                viewModel.stopSimulation()
                advanceUntilIdle()
                awaitItem() // State after stopSimulation

                viewModel.exportSimulationExcel(shareFile)
                advanceUntilIdle()

                assertEquals(excelBytes, sharedBytes)
                assertTrue(sharedFileName!!.startsWith("telemetry_"))
                assertTrue(sharedFileName!!.endsWith(".xlsx"))
                assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", sharedMimeType)
                assertNull(expectMostRecentItem().exportError)
            }
        }

    @Test
    fun `exportSimulationExcel handles empty telemetry`() =
        runTest {
            var sharedBytes: ByteArray? = null
            val shareFile: (ByteArray, String, String) -> Unit = { bytes, fileName, mimeType ->
                sharedBytes = bytes
            }

            viewModel.state.test {
                skipItems(1) // Skip initial state

                viewModel.exportSimulationExcel(shareFile)
                advanceUntilIdle()

                val emittedState = awaitItem()
                assertEquals("No telemetry data to export", emittedState.exportError)
                assertNull(sharedBytes)
            }
        }

    @Test
    fun `exportSimulationExcel handles export failure`() =
        runTest {
            val runId = "export-fail-111"
            val frames =
                listOf(
                    TelemetryFrame(
                        timestamp = Instant.parse("2024-01-01T00:00:01Z"),
                        pressure = PressureReading(1.0),
                        displacement = NozzleDisplacement(1.0),
                        flowRate = FlowRate(1.0),
                        temperature = Temperature(1.0),
                        viscosity = ViscosityEstimation(1.0),
                        pid = PIDState(1.0, 1.0, 1.0),
                        mpc = MPCPrediction(1, 1.0),
                    ),
                )
            val errorMessage = "Something went wrong during Excel generation"
            var sharedBytes: ByteArray? = null
            val shareFile: (ByteArray, String, String) -> Unit = { bytes, fileName, mimeType ->
                sharedBytes = bytes
            }

            every { telemetryExcelExporter.toXlsxBytes(any(), any(), any()) } throws RuntimeException(errorMessage)

            viewModel.state.test {
                skipItems(1) // Skip initial state

                // Set up a simulated run with telemetry for export
                viewModel.setSimulatedRunEnabled(true)
                viewModel.startStreaming(runId)
                frames.forEach { telemetryStream.emit(it) }
                advanceUntilIdle()
                awaitItem() // State after streaming starts
                awaitItem() // State after telemetry frames

                viewModel.stopSimulation()
                advanceUntilIdle()
                awaitItem() // State after stopSimulation

                viewModel.exportSimulationExcel(shareFile)
                advanceUntilIdle()

                val emittedState = awaitItem()
                assertTrue(emittedState.exportError!!.contains(errorMessage))
                assertNull(sharedBytes)
            }
        }
}
