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
    COMMERCIAL
}

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val deviceAuthorization: DeviceAuthorization? = null,
    val statusMessage: String? = null
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
    val commercialPipeline: CommercialPipeline = CommercialPipeline(),
    val selectedOpportunity: CommercialOpportunity? = null,
    val commercialError: String? = null,
    val errorBanner: String? = null,
    val streamStatus: String? = null,
    val auth: AuthUiState = AuthUiState(),
    val statusMessage: String? = null
)
