package com.neogenesis.platform.control.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RegenOpsApp(
    viewModel: RegenOpsViewModel,
    openExternalUrl: (String) -> Unit,
    shareFile: (ByteArray, String, String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("RegenOps Control", style = MaterialTheme.typography.headlineSmall)
        if (state.demoModeEnabled) {
            Text("DEMO mode enabled", color = MaterialTheme.colorScheme.primary)
        }
        if (state.simulatedRunEnabled) {
            Text("Simulated run mode enabled (Digital Twin)", color = MaterialTheme.colorScheme.primary)
        }
        state.errorBanner?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.streamStatus?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (!state.auth.isAuthenticated) {
            AuthPanel(
                state = state.auth,
                onStart = { viewModel.beginDeviceAuth { } },
                onOpen = { url -> openExternalUrl(url) },
                onPoll = { viewModel.pollDeviceAuth() }
            )
            state.auth.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            return
        }

        NavigationRow(
            screen = state.screen,
            onSelect = { viewModel.setScreen(it) },
            onLogout = { viewModel.logout() },
            showCommercial = state.commercialModeEnabled,
            showExports = state.founderModeEnabled || state.demoModeEnabled,
            showTrace = state.founderModeEnabled || state.traceModeEnabled || state.demoModeEnabled
        )

        when (state.screen) {
            AppScreen.PROTOCOLS -> ProtocolsScreen(
                protocols = state.protocols,
                query = state.protocolQuery,
                onQueryChange = viewModel::updateQuery,
                onSelect = {
                    viewModel.selectProtocol(it)
                    viewModel.setScreen(AppScreen.PROTOCOL_DETAIL)
                },
                onRefresh = { viewModel.refreshProtocols() }
            )
            AppScreen.PROTOCOL_DETAIL -> ProtocolDetailScreen(
                protocol = state.selectedProtocol,
                selectedVersion = state.selectedVersion,
                onBack = { viewModel.setScreen(AppScreen.PROTOCOLS) },
                onSelectVersion = viewModel::selectVersion,
                onPublish = { viewModel.publishSelectedVersion() }
            )
            AppScreen.RUN_CONTROL -> RunControlScreen(
                protocols = state.protocols,
                selectedProtocol = state.selectedProtocol,
                selectedVersion = state.selectedVersion,
                runs = state.runs,
                demoModeEnabled = state.demoModeEnabled,
                simulatedRunEnabled = state.simulatedRunEnabled,
                onSimulatedRunToggle = viewModel::setSimulatedRunEnabled,
                onSelectProtocol = viewModel::selectProtocol,
                onSelectVersion = viewModel::selectVersion,
                onStartRun = viewModel::startRun,
                onStartDemoRun = viewModel::startDemoRun,
                onPauseRun = { viewModel.pauseRun() },
                onAbortRun = { viewModel.abortRun() },
                onSelectRun = {
                    viewModel.selectRun(it)
                    viewModel.setScreen(AppScreen.LIVE_RUN)
                    viewModel.startStreaming(it)
                },
                onRefreshRuns = viewModel::refreshRuns
            )
            AppScreen.LIVE_RUN -> LiveRunScreen(
                runId = state.selectedRunId,
                runEvents = state.runEvents,
                telemetryFrames = state.telemetryFrames
            )
            AppScreen.AUTH -> AuthPanel(
                state = state.auth,
                onStart = { viewModel.beginDeviceAuth { } },
                onOpen = { url -> openExternalUrl(url) },
                onPoll = { viewModel.pollDeviceAuth() }
            )
            AppScreen.COMMERCIAL -> CommercialPipelineScreen(
                pipeline = state.commercialPipeline,
                selected = state.selectedOpportunity,
                error = state.commercialError,
                onSelect = { viewModel.selectOpportunity(it) },
                onExport = { viewModel.exportCommercialCsv { bytes -> shareFile(bytes, "commercial_pipeline.csv", "text/csv") } },
                onRefresh = { viewModel.loadCommercialPipeline() }
            )
            AppScreen.EXPORTS -> ExportsScreen(
                runId = state.export.runId,
                onRunIdChange = viewModel::updateExportRunId,
                isLoading = state.export.isLoading,
                statusMessage = state.export.statusMessage,
                errorMessage = state.export.errorMessage,
                onExportReport = { viewModel.exportRunReport { bytes, name, type -> shareFile(bytes, name, type) } },
                onExportAudit = { viewModel.exportAuditBundle { bytes, name, type -> shareFile(bytes, name, type) } }
            )
            AppScreen.TRACE -> TraceScreen(
                score = state.trace.score,
                alerts = state.trace.alerts,
                isLoading = state.trace.isLoading,
                statusMessage = state.trace.statusMessage,
                errorMessage = state.trace.errorMessage,
                onRefresh = { viewModel.loadTraceSummary() }
            )
        }

        state.statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun NavigationRow(
    screen: AppScreen,
    onSelect: (AppScreen) -> Unit,
    onLogout: () -> Unit,
    showCommercial: Boolean,
    showExports: Boolean,
    showTrace: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { onSelect(AppScreen.PROTOCOLS) }) { Text("Protocols") }
            Button(onClick = { onSelect(AppScreen.RUN_CONTROL) }) { Text("Run Control") }
            Button(onClick = { onSelect(AppScreen.LIVE_RUN) }) { Text("Live Run") }
            if (showCommercial) {
                Button(onClick = { onSelect(AppScreen.COMMERCIAL) }) { Text("Commercial") }
            }
            if (showExports) {
                Button(onClick = { onSelect(AppScreen.EXPORTS) }) { Text("Exports") }
            }
            if (showTrace) {
                Button(onClick = { onSelect(AppScreen.TRACE) }) { Text("Trace") }
            }
            Button(onClick = onLogout) { Text("Logout") }
        }
    }
}

@Composable
private fun AuthPanel(
    state: AuthUiState,
    onStart: () -> Unit,
    onOpen: (String) -> Unit,
    onPoll: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("OIDC Device Login", style = MaterialTheme.typography.titleLarge)
            if (state.deviceAuthorization == null) {
                Text("Start device authorization to receive a user code.")
                Button(onClick = onStart) { Text("Start login") }
            } else {
                val device = state.deviceAuthorization
                Text("User Code: ${device.userCode}")
                Text("Verification URL: ${device.verificationUri}")
                device.verificationUriComplete?.let { Text("Direct URL: $it") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val url = device.verificationUriComplete ?: device.verificationUri
                        onOpen(url)
                    }) { Text("Open browser") }
                    Button(onClick = onPoll) { Text("I've authorized") }
                }
            }
            state.statusMessage?.let { Text(it) }
        }
    }
}
