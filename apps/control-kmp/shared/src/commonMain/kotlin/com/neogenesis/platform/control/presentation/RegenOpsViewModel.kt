package com.neogenesis.platform.control.presentation

import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.data.RegenOpsRepository
import com.neogenesis.platform.control.data.remote.CommercialApi
import com.neogenesis.platform.control.data.oidc.DeviceAuthorization
import com.neogenesis.platform.control.data.oidc.OidcConfig
import com.neogenesis.platform.control.data.oidc.OidcRepository
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
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
    private val commercialApi: CommercialApi
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var eventsJob: Job? = null
    private var telemetryJob: Job? = null

    private val _state = MutableStateFlow(
        RegenOpsUiState(
            screen = AppScreen.PROTOCOLS,
            auth = AuthUiState(isAuthenticated = oidcRepository.hasTokens()),
            commercialModeEnabled = System.getenv("COMMERCIAL_MODE") == "true"
        )
    )
    val state: StateFlow<RegenOpsUiState> = _state

    init {
        scope.launch {
            repository.protocols.collectLatest { protocols ->
                _state.update { current ->
                    val selected = resolveSelectedProtocol(current.selectedProtocol, protocols)
                    val selectedVersion = resolveSelectedVersion(current.selectedVersion, selected)
                    current.copy(
                        protocols = protocols,
                        selectedProtocol = selected,
                        selectedVersion = selectedVersion
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

    fun setScreen(screen: AppScreen) {
        _state.update { it.copy(screen = screen) }
        if (screen != AppScreen.LIVE_RUN) {
            stopStreaming()
        }
        if (screen == AppScreen.COMMERCIAL) {
            loadCommercialPipeline()
        }
    }

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
        _state.update { it.copy(selectedRunId = runId) }
    }

    fun selectOpportunity(opportunity: CommercialOpportunity) {
        _state.update { it.copy(selectedOpportunity = opportunity) }
    }

    fun refreshProtocols() {
        scope.launch {
            when (val result = repository.refreshProtocols()) {
                is ApiResult.Success -> _state.update { it.copy(statusMessage = "Protocols refreshed", errorBanner = null) }
                is ApiResult.Failure -> _state.update {
                    it.copy(errorBanner = mapNetworkError(result.error, "Failed to refresh protocols"))
                }
            }
        }
    }

    fun refreshRuns() {
        scope.launch {
            when (val result = repository.refreshRuns()) {
                is ApiResult.Success -> _state.update { it.copy(statusMessage = "Runs refreshed", errorBanner = null) }
                is ApiResult.Failure -> _state.update { it.copy(statusMessage = "Runs refresh not supported yet") }
            }
        }
    }

    fun publishSelectedVersion() {
        val version = _state.value.selectedVersion ?: return
        scope.launch {
            when (val result = repository.publishVersion(version.protocolId.value, version.id.value)) {
                is ApiResult.Success -> _state.update { it.copy(statusMessage = "Version published", errorBanner = null) }
                is ApiResult.Failure -> _state.update {
                    it.copy(errorBanner = mapNetworkError(result.error, "Publish failed"))
                }
            }
        }
    }

    fun startRun() {
        val protocolId = _state.value.selectedProtocol?.id?.value
        val versionId = _state.value.selectedVersion?.id?.value
        if (protocolId == null || versionId == null) {
            _state.update { it.copy(statusMessage = "Select a protocol version first") }
            return
        }
        scope.launch {
            when (val result = repository.startRun(protocolId, versionId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(selectedRunId = result.value.id.value, statusMessage = "Run started", errorBanner = null) }
                    setScreen(AppScreen.LIVE_RUN)
                    startStreaming(result.value.id.value)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(errorBanner = mapNetworkError(result.error, "Start run failed"))
                }
            }
        }
    }

    fun pauseRun() {
        val runId = _state.value.selectedRunId ?: _state.value.runs.firstOrNull()?.id?.value
        if (runId == null) return
        scope.launch {
            when (val result = repository.updateRunStatus(runId, RunStatus.PAUSED)) {
                is ApiResult.Success -> _state.update { it.copy(statusMessage = "Run paused", errorBanner = null) }
                is ApiResult.Failure -> _state.update {
                    it.copy(errorBanner = mapNetworkError(result.error, "Pause failed"))
                }
            }
        }
    }

    fun abortRun() {
        val runId = _state.value.selectedRunId ?: _state.value.runs.firstOrNull()?.id?.value
        if (runId == null) return
        scope.launch {
            when (val result = repository.updateRunStatus(runId, RunStatus.ABORTED)) {
                is ApiResult.Success -> _state.update { it.copy(statusMessage = "Run aborted", errorBanner = null) }
                is ApiResult.Failure -> _state.update {
                    it.copy(errorBanner = mapNetworkError(result.error, "Abort failed"))
                }
            }
        }
    }

    fun startStreaming(runId: String) {
        stopStreaming()
        _state.update { it.copy(streamStatus = "Connecting...") }
        eventsJob = scope.launch {
            repository.streamEvents(runId).collectLatest { event ->
                _state.update { current ->
                    val events = listOf(event) + current.runEvents
                    current.copy(runEvents = events.take(120), streamStatus = null)
                }
            }
        }
        telemetryJob = scope.launch {
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
                is ApiResult.Failure -> _state.update { current ->
                    current.copy(auth = current.auth.copy(statusMessage = "OIDC device auth failed"))
                }
            }
        }
    }

    fun pollDeviceAuth() {
        val device = _state.value.auth.deviceAuthorization ?: return
        val issuer = config.oidcIssuer
        val clientId = config.oidcClientId
        if (issuer.isBlank() || clientId.isBlank()) return
        scope.launch {
            val result = oidcRepository.pollForTokens(
                OidcConfig(issuer, clientId, config.oidcAudience),
                device.deviceCode,
                device.intervalSeconds
            )
            when (result) {
                is ApiResult.Success -> {
                    _state.update { current ->
                        current.copy(
                            auth = current.auth.copy(
                                isAuthenticated = true,
                                deviceAuthorization = null,
                                statusMessage = "Authenticated"
                            )
                        )
                    }
                    refreshProtocols()
                    refreshRuns()
                }
                is ApiResult.Failure -> _state.update { current ->
                    current.copy(auth = current.auth.copy(statusMessage = "Authentication failed"))
                }
            }
        }
    }

    fun logout() {
        oidcRepository.clearTokens()
        _state.update { it.copy(auth = AuthUiState(isAuthenticated = false), statusMessage = "Logged out") }
    }

    fun loadCommercialPipeline() {
        if (!_state.value.commercialModeEnabled) return
        scope.launch {
            when (val result = commercialApi.fetchPipeline()) {
                is ApiResult.Success -> _state.update { it.copy(commercialPipeline = result.value, commercialError = null) }
                is ApiResult.Failure -> _state.update { it.copy(commercialError = "Commercial pipeline unavailable") }
            }
        }
    }

    fun exportCommercialCsv(onExport: (ByteArray) -> Unit) {
        if (!_state.value.commercialModeEnabled) return
        scope.launch {
            when (val result = commercialApi.exportCsv()) {
                is ApiResult.Success -> onExport(result.value)
                is ApiResult.Failure -> _state.update {
                    it.copy(commercialError = "Unable to export CSV")
                }
            }
        }
    }

    fun close() {
        repository.close()
        scope.cancel()
    }

    private fun resolveSelectedProtocol(current: Protocol?, protocols: List<Protocol>): Protocol? {
        return protocols.firstOrNull { it.id == current?.id } ?: protocols.firstOrNull()
    }

    private fun resolveSelectedVersion(current: ProtocolVersion?, protocol: Protocol?): ProtocolVersion? {
        if (protocol == null) return null
        return protocol.versions.firstOrNull { it.id == current?.id }
            ?: protocol.latestVersion
            ?: protocol.versions.firstOrNull()
    }

    private fun mapNetworkError(error: NetworkError, fallback: String): String {
        return when (error) {
            is NetworkError.ConnectivityError -> "Server unreachable"
            is NetworkError.TimeoutError -> "Request timed out"
            is NetworkError.HttpError -> if (error.statusCode == 401) "Auth expired. Please login again." else fallback
            is NetworkError.SerializationError -> "Invalid response from server"
            is NetworkError.UnknownError -> fallback
        }
    }
}
