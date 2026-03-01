package com.neogenesis.platform.control.presentation

import com.neogenesis.platform.control.data.oidc.DeviceAuthorization
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.telemetry.TelemetryFrame

enum class AppScreen {
    PROTOCOLS,
    PROTOCOL_DETAIL,
    RUN_CONTROL,
    LIVE_RUN,
    AUTH,
    COMMERCIAL,
    EXPORTS,
    TRACE,
    UNSUPPORTED
}

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val deviceAuthorization: DeviceAuthorization? = null,
    val statusMessage: String? = null
)

data class ExportUiState(
    val runId: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

data class DriftAlert(
    val id: String,
    val title: String,
    val severity: String,
    val message: String
)

data class TraceUiState(
    val score: Int? = null,
    val alerts: List<DriftAlert> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

data class RegenOpsUiState(
    val screen: AppScreen = AppScreen.PROTOCOLS,
    val protocols: List<Protocol> = emptyList(),
    val runs: List<Run> = emptyList(),
    val selectedProtocol: Protocol? = null,
    val selectedVersion: ProtocolVersion? = null,
    val selectedRunId: String? = null,
    val protocolQuery: String = "",
    val runEvents: List<RunEvent> = emptyList(),
    val telemetryFrames: List<TelemetryFrame> = emptyList(),
    val commercialModeEnabled: Boolean = false,
    val founderModeEnabled: Boolean = false,
    val traceModeEnabled: Boolean = false,
    val demoModeEnabled: Boolean = false,
    val commercialPipeline: CommercialPipeline = CommercialPipeline(),
    val selectedOpportunity: CommercialOpportunity? = null,
    val commercialError: String? = null,
    val export: ExportUiState = ExportUiState(),
    val trace: TraceUiState = TraceUiState(),
    val simulatedRunEnabled: Boolean = false,
    val isStartingRun: Boolean = false,
    val isCreatingProtocol: Boolean = false,
    val isUpdatingProtocolStatus: Boolean = false,
    val errorBanner: String? = null,
    val streamStatus: String? = null,
    val auth: AuthUiState = AuthUiState(),
    val statusMessage: String? = null,
    val devicePolicy: com.neogenesis.platform.control.device.DevicePolicyState? = null,
    val unsupportedMessage: String? = null
)
