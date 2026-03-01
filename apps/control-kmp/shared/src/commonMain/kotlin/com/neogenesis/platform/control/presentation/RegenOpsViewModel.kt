package com.neogenesis.platform.control.presentation

import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.remote.CommercialApi
import com.neogenesis.platform.control.data.remote.CreateProtocolRequest
import com.neogenesis.platform.control.data.remote.ExportsApi
import com.neogenesis.platform.control.data.remote.SimulatorApi
import com.neogenesis.platform.control.data.remote.TraceApi
import com.neogenesis.platform.control.data.oidc.DeviceAuthorization
import com.neogenesis.platform.control.data.oidc.OidcConfig
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.LogLevel
import com.neogenesis.platform.shared.network.Redaction
import com.neogenesis.platform.shared.network.NetworkError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegenOpsViewModel(
    private val config: AppConfig,
    private val repository: RegenOpsRepository,
    private val oidcRepository: OidcRepository,
    private val commercialApi: CommercialApi,
    private val exportsApi: ExportsApi,
    private val traceApi: TraceApi,
    private val simulatorApi: SimulatorApi,
    private val logger: AppLogger,
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var eventsJob: Job? = null
    private var telemetryJob: Job? = null
    private var protocolExtras: Map<String, Protocol> = emptyMap()

    /** Simple back stack for “detail” navigation (not the bottom tabs). */
    private val backStack: ArrayDeque<AppScreen> = ArrayDeque()

    private val _state =
        MutableStateFlow(
            RegenOpsUiState(
                screen = AppScreen.PROTOCOLS,
                auth = AuthUiState(isAuthenticated = oidcRepository.hasTokens()),
                commercialModeEnabled = config.commercialModeEnabled,
                founderModeEnabled = config.founderModeEnabled,
                traceModeEnabled = config.traceModeEnabled,
                demoModeEnabled = config.demoModeEnabled,
            ),
        )
    val state: StateFlow<RegenOpsUiState> = _state

    init {
        scope.launch {
            repository.protocols.collectLatest { protocols ->
                _state.update { current ->
                    val effectiveProtocols =
                        if (protocols.isEmpty()) {
                            // Keep the UI usable when backend returns empty.
                            MockProtocols.sample()
                        } else {
                            protocols.map { protocol ->
                                val extra = protocolExtras[protocol.id.value]
                                if (extra == null) protocol
                                else protocol.copy(
                                    status = extra.status,
                                    resultSummary = extra.resultSummary,
                                    resultMetrics = extra.resultMetrics,
                                    lastOutcome = extra.lastOutcome,
                                    evidenceSummary = extra.evidenceSummary,
                                    lastRunTimeline = extra.lastRunTimeline,
                                    evidenceArtifacts = extra.evidenceArtifacts
                                )
                            }
                        }

                    val selected = resolveSelectedProtocol(current.selectedProtocol, effectiveProtocols)
                    val selectedVersion = resolveSelectedVersion(current.selectedVersion, selected)

                    current.copy(
                        protocols = effectiveProtocols,
                        selectedProtocol = selected,
                        selectedVersion = selectedVersion,
                    )
                }
            }
        }

        scope.launch {
            repository.runs.collectLatest { runs ->
                _state.update { current -> current.copy(runs = runs) }
            }
        }

        if (oidcRepository.hasTokens()) {
            refreshProtocols()
            refreshRuns()
        }
    }

    // -----------------------------------------------------------------------------
    // Navigation (Back)
    // -----------------------------------------------------------------------------
    fun canGoBack(): Boolean = backStack.isNotEmpty()

    fun navigateTo(screen: AppScreen) {
        val current = _state.value.screen
        if (current != screen) backStack.addLast(current)
        setScreenInternal(screen)
    }

    fun navigateBack() {
        val prev = backStack.removeLastOrNull() ?: return
        setScreenInternal(prev)
    }

    /** Used by bottom nav / rail (treat as “top level” -> clears back stack). */
    fun setScreen(screen: AppScreen) {
        backStack.clear()
        setScreenInternal(screen)
    }

    private fun setScreenInternal(screen: AppScreen) {
        _state.update { it.copy(screen = screen) }

        // Leaving live stream screen stops streaming
        if (screen != AppScreen.LIVE_RUN) stopStreaming()

        if (screen == AppScreen.COMMERCIAL) loadCommercialPipeline()

        if (screen == AppScreen.EXPORTS) {
            _state.update { current ->
                if (current.export.runId.isNotBlank()) current
                else current.copy(export = current.export.copy(runId = current.selectedRunId ?: ""))
            }
        }

        if (screen == AppScreen.TRACE) loadTraceSummary()
    }

    // -----------------------------------------------------------------------------
    // UI state edits
    // -----------------------------------------------------------------------------
    fun updateQuery(query: String) {
        _state.update { it.copy(protocolQuery = query) }
    }

    fun selectProtocol(protocol: Protocol) {
        _state.update {
            val version = protocol.latestVersion ?: protocol.versions.firstOrNull()
            it.copy(selectedProtocol = protocol, selectedVersion = version)
        }
    }

    fun selectVersion(version: ProtocolVersion) {
        _state.update { it.copy(selectedVersion = version) }
    }

    fun selectRun(runId: String) {
        _state.update { current ->
            current.copy(selectedRunId = runId, export = current.export.copy(runId = runId))
        }
    }

    fun updateExportRunId(runId: String) {
        _state.update { it.copy(export = it.export.copy(runId = runId)) }
    }

    fun setSimulatedRunEnabled(enabled: Boolean) {
        logger.log(
            LogLevel.INFO,
            "Simulated run toggle",
            mapOf("enabled" to enabled.toString())
        )
        _state.update { current ->
            current.copy(
                simulatedRunEnabled = enabled,
                statusMessage = if (enabled) "Simulated run mode enabled" else "Simulated run mode disabled",
            )
        }
    }

    fun selectOpportunity(opportunity: CommercialOpportunity) {
        _state.update { it.copy(selectedOpportunity = opportunity) }
    }

    fun clearSelectedOpportunity() {
        _state.update { it.copy(selectedOpportunity = null) }
    }

    // -----------------------------------------------------------------------------
    // Protocols / Runs
    // -----------------------------------------------------------------------------
    fun refreshProtocols() {
        scope.launch {
            when (val result = repository.refreshProtocols()) {
                is ApiResult.Success -> {
                    protocolExtras = result.value.associateBy { it.id.value }
                    _state.update { it.copy(statusMessage = "Protocols refreshed", errorBanner = null) }
                }
                is ApiResult.Failure ->
                    _state.update { it.copy(errorBanner = mapNetworkError(result.error, "Failed to refresh protocols")) }
            }
        }
    }

    fun createProtocol(request: CreateProtocolRequest) {
        if (_state.value.isCreatingProtocol) return
        _state.update { it.copy(isCreatingProtocol = true, errorBanner = null) }
        scope.launch {
            when (val result = repository.createProtocol(request)) {
                is ApiResult.Success -> {
                    protocolExtras = protocolExtras + (result.value.id.value to result.value)
                    _state.update {
                        it.copy(
                            statusMessage = "Protocol created",
                            isCreatingProtocol = false,
                            errorBanner = null,
                            selectedProtocol = result.value,
                            selectedVersion = result.value.latestVersion
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _state.update {
                        it.copy(
                            isCreatingProtocol = false,
                            errorBanner = mapNetworkError(result.error, "Failed to create protocol")
                        )
                    }
                }
            }
        }
    }

    fun refreshRuns() {
        scope.launch {
            when (val result = repository.refreshRuns()) {
                is ApiResult.Success ->
                    _state.update { it.copy(statusMessage = "Runs refreshed", errorBanner = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(errorBanner = mapNetworkError(result.error, "Failed to refresh runs")) }
            }
        }
    }

    fun publishSelectedVersion() {
        val version = _state.value.selectedVersion ?: return
        scope.launch {
            when (val result = repository.publishVersion(version.protocolId.value, version.id.value)) {
                is ApiResult.Success ->
                    _state.update { it.copy(statusMessage = "Version published", errorBanner = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(errorBanner = mapNetworkError(result.error, "Publish failed")) }
            }
        }
    }

    fun startRun() {
        if (_state.value.isStartingRun) {
            _state.update { it.copy(statusMessage = "Run already starting...") }
            return
        }
        val protocolId = _state.value.selectedProtocol?.id?.value
        val versionId = _state.value.selectedVersion?.id?.value
        if (protocolId == null || versionId == null) {
            _state.update { it.copy(statusMessage = "Select a protocol version first") }
            return
        }

        logger.log(LogLevel.INFO, "Start mission clicked", mapOf("protocolId" to protocolId, "versionId" to versionId))
        _state.update { it.copy(isStartingRun = true, errorBanner = null) }

        scope.launch {
            try {
                logger.log(LogLevel.DEBUG, "Start run API call", mapOf("protocolId" to protocolId, "versionId" to versionId))
                when (val result = repository.startRun(protocolId, versionId)) {
                    is ApiResult.Success -> {
                        val runId = result.value.id.value
                        val message =
                            if (_state.value.simulatedRunEnabled) "Simulated run requested" else "Run started"
                        logger.log(LogLevel.INFO, "Run started", mapOf("runId" to Redaction.value(runId)))
                        _state.update {
                            it.copy(
                                selectedRunId = runId,
                                statusMessage = message,
                                errorBanner = null,
                                isStartingRun = false
                            )
                        }

                        navigateTo(AppScreen.LIVE_RUN)
                        startStreaming(runId)
                    }

                    is ApiResult.Failure -> {
                        val errorMessage = mapNetworkError(result.error, "Start run failed")
                        logger.log(LogLevel.WARN, "Start run failed", mapOf("error" to errorMessage))
                        _state.update { it.copy(errorBanner = errorMessage, isStartingRun = false) }
                    }
                }
            } catch (err: Throwable) {
                logger.log(LogLevel.ERROR, "Start run crashed", mapOf("error" to (err.message ?: "unknown")))
                _state.update { it.copy(errorBanner = "Start run failed: ${err.message ?: "unknown"}", isStartingRun = false) }
            }
        }
    }

    fun startDemoRun() {
        if (!_state.value.demoModeEnabled) return
        startRun()
    }

    fun startSimulatedRun(config: SimulationConfig) {
        if (_state.value.isStartingRun) {
            _state.update { it.copy(statusMessage = "Simulation already starting...") }
            return
        }
        val protocolId = _state.value.selectedProtocol?.id?.value ?: "sim-protocol"
        if (!_state.value.simulatedRunEnabled) setSimulatedRunEnabled(true)

        logger.log(
            LogLevel.INFO,
            "Start simulation requested",
            mapOf(
                "protocolId" to protocolId,
                "durationMinutes" to config.durationMinutes.toString(),
                "tickMillis" to config.tickMillis.toString()
            )
        )

        _state.update {
            it.copy(
                statusMessage =
                    "Starting simulation: ${config.durationMinutes}m @${config.speedFactor}x (${config.tickMillis}ms tick)",
                isStartingRun = true,
                errorBanner = null
            )
        }

        scope.launch {
            try {
                logger.log(LogLevel.DEBUG, "Start simulation API call", mapOf("protocolId" to protocolId))
                when (val result = simulatorApi.startSimulatedRun(protocolId, config)) {
                    is ApiResult.Success -> {
                        val runId = result.value
                        logger.log(LogLevel.INFO, "Simulation started", mapOf("runId" to Redaction.value(runId)))
                        _state.update {
                            it.copy(
                                selectedRunId = runId,
                                statusMessage = "Simulation started",
                                errorBanner = null,
                                isStartingRun = false
                            )
                        }
                        navigateTo(AppScreen.LIVE_RUN)
                        startStreaming(runId)
                    }
                    is ApiResult.Failure -> {
                        val errorMessage = mapNetworkError(result.error, "Start simulation failed")
                        logger.log(LogLevel.WARN, "Start simulation failed", mapOf("error" to errorMessage))
                        _state.update { it.copy(errorBanner = errorMessage, isStartingRun = false) }
                    }
                }
            } catch (err: Throwable) {
                logger.log(LogLevel.ERROR, "Start simulation crashed", mapOf("error" to (err.message ?: "unknown")))
                _state.update { it.copy(errorBanner = "Start simulation failed: ${err.message ?: "unknown"}", isStartingRun = false) }
            }
        }
    }

    fun pauseRun() {
        val runId = _state.value.selectedRunId ?: _state.value.runs.firstOrNull()?.id?.value ?: return
        scope.launch {
            when (val result = repository.updateRunStatus(runId, RunStatus.PAUSED)) {
                is ApiResult.Success ->
                    _state.update { it.copy(statusMessage = "Run paused", errorBanner = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(errorBanner = mapNetworkError(result.error, "Pause failed")) }
            }
        }
    }

    fun abortRun() {
        val runId = _state.value.selectedRunId ?: _state.value.runs.firstOrNull()?.id?.value ?: return
        scope.launch {
            when (val result = repository.updateRunStatus(runId, RunStatus.ABORTED)) {
                is ApiResult.Success ->
                    _state.update { it.copy(statusMessage = "Run stopped", errorBanner = null) }
                is ApiResult.Failure ->
                    _state.update { it.copy(errorBanner = mapNetworkError(result.error, "Stop failed")) }
            }
        }
    }

    // -----------------------------------------------------------------------------
    // Streaming
    // -----------------------------------------------------------------------------
    fun startStreaming(runId: String) {
        stopStreaming()
        _state.update { it.copy(streamStatus = "Connecting...") }

        eventsJob =
            scope.launch {
                repository.streamEvents(runId).collectLatest { event ->
                    _state.update { current ->
                        val events = listOf(event) + current.runEvents
                        current.copy(runEvents = events.take(120), streamStatus = null)
                    }
                }
            }

        telemetryJob =
            scope.launch {
                repository.streamTelemetry(runId).collectLatest { frame ->
                    _state.update { current ->
                        val telemetry = current.telemetryFrames + frame
                        current.copy(telemetryFrames = telemetry.takeLast(200), streamStatus = null)
                    }
                }
            }
    }

    fun stopStreaming() {
        eventsJob?.cancel()
        telemetryJob?.cancel()
        eventsJob = null
        telemetryJob = null
    }

    // -----------------------------------------------------------------------------
    // Auth
    // -----------------------------------------------------------------------------
    fun beginDeviceAuth(onDeviceCode: (DeviceAuthorization) -> Unit) {
        val issuer = config.oidcIssuer
        val clientId = config.oidcClientId
        if (issuer.isBlank() || clientId.isBlank()) {
            _state.update { it.copy(auth = it.auth.copy(statusMessage = "OIDC config missing")) }
            return
        }

        scope.launch {
            val result = oidcRepository.startDeviceAuthorization(OidcConfig(issuer, clientId, config.oidcAudience))
            when (result) {
                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(auth = current.auth.copy(deviceAuthorization = result.value, statusMessage = null))
                    }
                    onDeviceCode(result.value)
                }
                is ApiResult.Failure ->
                    _state.update { current -> current.copy(auth = current.auth.copy(statusMessage = "OIDC device auth failed")) }
            }
        }
    }

    fun pollDeviceAuth() {
        val device = _state.value.auth.deviceAuthorization ?: return
        val issuer = config.oidcIssuer
        val clientId = config.oidcClientId
        if (issuer.isBlank() || clientId.isBlank()) return

        scope.launch {
            val result =
                oidcRepository.pollForTokens(
                    OidcConfig(issuer, clientId, config.oidcAudience),
                    device.deviceCode,
                    device.intervalSeconds,
                )

            when (result) {
                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            auth = current.auth.copy(
                                isAuthenticated = true,
                                deviceAuthorization = null,
                                statusMessage = "Authenticated",
                            ),
                        )
                    }
                    refreshProtocols()
                    refreshRuns()
                }

                is ApiResult.Failure ->
                    _state.update { current -> current.copy(auth = current.auth.copy(statusMessage = "Authentication failed")) }
            }
        }
    }

    fun logout() {
        oidcRepository.clearTokens()
        backStack.clear()
        stopStreaming()
        _state.update { it.copy(auth = AuthUiState(isAuthenticated = false), statusMessage = "Logged out") }
    }

    // -----------------------------------------------------------------------------
    // Commercial / Trace
    // -----------------------------------------------------------------------------
    fun loadCommercialPipeline() {
        if (!_state.value.commercialModeEnabled) return
        scope.launch {
            when (val result = commercialApi.fetchPipeline()) {
                is ApiResult.Success -> _state.update { it.copy(commercialPipeline = result.value, commercialError = null) }
                is ApiResult.Failure -> _state.update { it.copy(commercialError = "Commercial pipeline unavailable") }
            }
        }
    }

    fun loadTraceSummary() {
        if (!_state.value.founderModeEnabled && !_state.value.traceModeEnabled && !_state.value.demoModeEnabled) return

        scope.launch {
            _state.update { current ->
                current.copy(trace = current.trace.copy(isLoading = true, statusMessage = "Loading trace metrics...", errorMessage = null))
            }

            val scoreResult = traceApi.getReproducibilityScore()
            val alertsResult = traceApi.listDriftAlerts()

            _state.update { current ->
                val score = (scoreResult as? ApiResult.Success)?.value ?: current.trace.score
                val alerts = (alertsResult as? ApiResult.Success)?.value ?: current.trace.alerts
                val error =
                    when {
                        scoreResult is ApiResult.Failure -> mapNetworkError(scoreResult.error, "Trace metrics unavailable")
                        alertsResult is ApiResult.Failure -> mapNetworkError(alertsResult.error, "Trace alerts unavailable")
                        else -> null
                    }

                current.copy(
                    trace = current.trace.copy(
                        score = score,
                        alerts = alerts,
                        isLoading = false,
                        statusMessage = if (error == null) "Trace metrics updated" else null,
                        errorMessage = error,
                    ),
                )
            }
        }
    }

    // -----------------------------------------------------------------------------
    // Exports (Report / PDF)
    // -----------------------------------------------------------------------------
    fun exportRunReport(onExport: (ByteArray, String, String) -> Unit) {
        if (!_state.value.founderModeEnabled && !_state.value.demoModeEnabled) {
            _state.update { it.copy(export = it.export.copy(errorMessage = "Exports require demo or founder mode")) }
            return
        }

        val runId = resolveExportRunId()
        if (runId == null) {
            _state.update { it.copy(export = it.export.copy(errorMessage = "Select a run ID first")) }
            return
        }

        scope.launch {
            _state.update { it.copy(export = it.export.copy(isLoading = true, statusMessage = "Exporting run report...", errorMessage = null)) }

            when (val result = exportsApi.exportRunReport(runId)) {
                is ApiResult.Success -> {
                    val safeRun = sanitizeFilePart(runId)
                    val payload = result.value
                    val suggested = payload.fileName?.let(::sanitizeFilePart).orEmpty()

                    val mimeType = payload.contentType ?: "application/octet-stream"
                    val defaultName =
                        when {
                            mimeType.contains("pdf", ignoreCase = true) -> "run_report_${safeRun}.pdf"
                            mimeType.contains("zip", ignoreCase = true) -> "run_report_${safeRun}.zip"
                            mimeType.contains("json", ignoreCase = true) -> "run_report_${safeRun}.json"
                            else -> "run_report_${safeRun}.bin"
                        }
                    val fileName = suggested.ifBlank { defaultName }

                    onExport(payload.bytes, fileName, mimeType)
                    _state.update { it.copy(export = it.export.copy(isLoading = false, statusMessage = "Run report ready", errorMessage = null)) }
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(export = it.export.copy(isLoading = false, errorMessage = mapNetworkError(result.error, "Export failed"))) }
            }
        }
    }

    fun exportAuditBundle(onExport: (ByteArray, String, String) -> Unit) {
        if (!_state.value.founderModeEnabled && !_state.value.demoModeEnabled) {
            _state.update { it.copy(export = it.export.copy(errorMessage = "Exports require demo or founder mode")) }
            return
        }

        val runId = resolveExportRunId()
        if (runId == null) {
            _state.update { it.copy(export = it.export.copy(errorMessage = "Select a run ID first")) }
            return
        }

        scope.launch {
            _state.update { it.copy(export = it.export.copy(isLoading = true, statusMessage = "Exporting audit bundle...", errorMessage = null)) }

            when (val result = exportsApi.exportAuditBundle(runId)) {
                is ApiResult.Success -> {
                    val safeRun = sanitizeFilePart(runId)
                    val payload = result.value
                    val suggested = payload.fileName?.let(::sanitizeFilePart).orEmpty()
                    val fileName = suggested.ifBlank { "audit_bundle_${safeRun}.zip" }
                    val mimeType = payload.contentType ?: "application/zip"

                    onExport(payload.bytes, fileName, mimeType)
                    _state.update { it.copy(export = it.export.copy(isLoading = false, statusMessage = "Audit bundle ready", errorMessage = null)) }
                }

                is ApiResult.Failure ->
                    _state.update { it.copy(export = it.export.copy(isLoading = false, errorMessage = mapNetworkError(result.error, "Export failed"))) }
            }
        }
    }

    fun exportCommercialCsv(onExport: (ByteArray) -> Unit) {
        if (!_state.value.commercialModeEnabled) return
        scope.launch {
            when (val result = commercialApi.exportCsv()) {
                is ApiResult.Success -> onExport(result.value)
                is ApiResult.Failure -> _state.update { it.copy(commercialError = "Unable to export CSV") }
            }
        }
    }

    // -----------------------------------------------------------------------------
    // Close
    // -----------------------------------------------------------------------------
    fun close() {
        repository.close()
        scope.cancel()
    }

    // -----------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------
    private fun resolveExportRunId(): String? {
        val candidate = _state.value.export.runId.ifBlank { _state.value.selectedRunId.orEmpty() }
        return candidate.takeIf { it.isNotBlank() }
    }

    private fun sanitizeFilePart(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    private fun resolveSelectedProtocol(current: Protocol?, protocols: List<Protocol>): Protocol? =
        protocols.firstOrNull { it.id == current?.id } ?: protocols.firstOrNull()

    private fun resolveSelectedVersion(current: ProtocolVersion?, protocol: Protocol?): ProtocolVersion? {
        if (protocol == null) return null
        return protocol.versions.firstOrNull { it.id == current?.id }
            ?: protocol.latestVersion
            ?: protocol.versions.firstOrNull()
    }

    private fun mapNetworkError(error: NetworkError, fallback: String): String =
        when (error) {
            is NetworkError.ConnectivityError -> "Server unreachable: ${error.message}"
            is NetworkError.TimeoutError -> "Request timed out"
            is NetworkError.HttpError ->
                when (error.status) {
                    401 -> "Auth expired. Please login again."
                    403 -> "Access denied."
                    404 -> "Not found."
                    in 500..599 -> "Server error. Try again later."
                    else -> fallback
                }
            is NetworkError.SerializationError -> "Invalid response from server"
            is NetworkError.UnknownError -> {
                val detail = error.message.ifBlank { "unknown_error" }
                val normalized = detail.lowercase()
                when {
                    normalized.contains("grpc_error") || normalized.contains("unreachable") || normalized.contains("connection") ->
                        "Server unreachable: $detail"
                    else -> "$fallback: $detail"
                }
            }
        }
}






