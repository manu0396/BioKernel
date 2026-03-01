package com.neogenesis.platform.control.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.control.device.CapabilityGate
import com.neogenesis.platform.control.presentation.design.NgCard
import com.neogenesis.platform.control.presentation.design.NgColors
import com.neogenesis.platform.control.presentation.design.NgMotion
import com.neogenesis.platform.control.presentation.design.NgPrimaryButton
import com.neogenesis.platform.control.presentation.design.NgScaffold
import com.neogenesis.platform.control.presentation.design.NgSpacing
import com.neogenesis.platform.control.presentation.design.NgStatus
import com.neogenesis.platform.control.presentation.design.NgStatusChip
import com.neogenesis.platform.control.presentation.design.NgTheme

@Composable
fun RegenOpsApp(
    viewModel: RegenOpsViewModel,
    openExternalUrl: (String) -> Unit,
    shareFile: (ByteArray, String, String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    NgTheme {
        val isDesktop = WindowSize.isDesktop()
        val canGoBack = viewModel.canGoBack()

        NgScaffold(
            title = "BioKernel Control",
            actions = {
                if (state.auth.isAuthenticated) {
                    IconButton(onClick = { viewModel.refreshProtocols() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            bottomBar = {
                if (!isDesktop && state.auth.isAuthenticated) {
                    NgBottomNavigation(state.screen, viewModel::setScreen, state)
                }
            },
        ) { paddingValues ->
            Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (isDesktop && state.auth.isAuthenticated) {
                    NgNavigationRail(state.screen, viewModel::setScreen, state)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = state.screen,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(NgMotion.Medium)) +
                                    slideInHorizontally(animationSpec = tween(NgMotion.Medium)) { it / 2 })
                                .togetherWith(fadeOut(animationSpec = tween(NgMotion.Fast)))
                        },
                    ) { screen ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(NgSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium),
                        ) {
                            state.errorBanner?.let { NgStatusChip(text = it, status = NgStatus.Error) }

                            Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                                if (state.traceModeEnabled) NgStatusChip(text = "TRACE ENABLED", status = NgStatus.Success)
                                if (state.demoModeEnabled) NgStatusChip(text = "DEMO MODE", status = NgStatus.Info)
                                if (state.simulatedRunEnabled) {
                                    NgStatusChip(
                                        text = "SIMULATION",
                                        status = NgStatus.Warning,
                                        onClick = { viewModel.setSimulatedRunEnabled(false) }
                                    )
                                } else {
                                    NgStatusChip(
                                        text = "SIMULATE",
                                        status = NgStatus.Warning,
                                        onClick = { viewModel.setSimulatedRunEnabled(true) }
                                    )
                                }
                            }

                            if (!state.auth.isAuthenticated) {
                                AuthScreen(
                                    state = state.auth,
                                    onStart = { viewModel.beginDeviceAuth { } },
                                    onOpen = openExternalUrl,
                                    onPoll = { viewModel.pollDeviceAuth() },
                                )
                            } else {
                                when (screen) {
                                    AppScreen.PROTOCOLS ->
                                        ProtocolsScreen(
                                            protocols = state.protocols,
                                            query = state.protocolQuery,
                                            onQueryChange = viewModel::updateQuery,
                                            isCreatingProtocol = state.isCreatingProtocol,
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                            onSelect = {
                                                viewModel.selectProtocol(it)
                                                viewModel.navigateTo(AppScreen.PROTOCOL_DETAIL)
                                            },
                                            onRefresh = viewModel::refreshProtocols,
                                            onCreateProtocol = viewModel::createProtocol,
                                        )

                                    AppScreen.PROTOCOL_DETAIL ->
                                        ProtocolDetailScreen(
                                            protocol = state.selectedProtocol,
                                            selectedVersion = state.selectedVersion,
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                            onSelectVersion = viewModel::selectVersion,
                                            onPublish = viewModel::publishSelectedVersion,
                                            onOpenExports = {
                                                state.selectedProtocol?.lastRunId?.let { runId ->
                                                    viewModel.updateExportRunId(runId)
                                                }
                                                viewModel.navigateTo(AppScreen.EXPORTS)
                                            },
                                            onUpdateStatus = viewModel::updateProtocolStatus,
                                            onDownloadReport = {
                                                val runId = state.selectedProtocol?.lastRunId
                                                if (runId != null) {
                                                    viewModel.updateExportRunId(runId)
                                                    viewModel.exportRunReport(shareFile)
                                                } else {
                                                    viewModel.updateStatusMessage("No run ID available for report.")
                                                }
                                            },
                                            onDownloadAudit = {
                                                val runId = state.selectedProtocol?.lastRunId
                                                if (runId != null) {
                                                    viewModel.updateExportRunId(runId)
                                                    viewModel.exportAuditBundle(shareFile)
                                                } else {
                                                    viewModel.updateStatusMessage("No run ID available for audit bundle.")
                                                }
                                            },
                                        )

                                    AppScreen.RUN_CONTROL ->
                                        RunControlScreen(
                                            protocols = state.protocols,
                                            selectedProtocol = state.selectedProtocol,
                                            selectedVersion = state.selectedVersion,
                                            runs = state.runs,
                                            demoModeEnabled = state.demoModeEnabled,
                                            simulatedRunEnabled = state.simulatedRunEnabled,
                                            isStartingRun = state.isStartingRun,
                                            onSimulatedRunToggle = viewModel::setSimulatedRunEnabled,
                                            onSelectProtocol = viewModel::selectProtocol,
                                            onSelectVersion = viewModel::selectVersion,
                                            onStartRun = viewModel::startRun,
                                            onStartSimulatedRun = viewModel::startSimulatedRun,
                                            onStartDemoRun = viewModel::startDemoRun,
                                            onPauseRun = viewModel::pauseRun,
                                            onAbortRun = viewModel::abortRun,
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                            onSelectRun = {
                                                viewModel.selectRun(it)
                                                viewModel.navigateTo(AppScreen.LIVE_RUN)
                                                viewModel.startStreaming(it)
                                            },
                                            onRefreshRuns = viewModel::refreshRuns,
                                        )

                                    AppScreen.LIVE_RUN ->
                                        LiveRunScreen(
                                            runId = state.selectedRunId,
                                            runEvents = state.runEvents,
                                            telemetryFrames = state.telemetryFrames,
                                            streamStatus = state.streamStatus,
                                            onPause = viewModel::pauseRun,
                                            onStop = viewModel::abortRun,
                                            onDownloadReport = {
                                                state.selectedRunId?.let { runId ->
                                                    viewModel.updateExportRunId(runId)
                                                    viewModel.exportRunReport(shareFile)
                                                }
                                            },
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                        )

                                    AppScreen.COMMERCIAL ->
                                        CommercialPipelineScreen(
                                            pipeline = state.commercialPipeline,
                                            selected = state.selectedOpportunity,
                                            error = state.commercialError,
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                            onSelect = viewModel::selectOpportunity,
                                            onDismissSelected = viewModel::clearSelectedOpportunity,
                                            onExport = {
                                                viewModel.exportCommercialCsv { bytes ->
                                                    shareFile(bytes, "commercial_pipeline.csv", "text/csv")
                                                }
                                            },
                                            onRefresh = viewModel::loadCommercialPipeline,
                                        )

                                    AppScreen.EXPORTS ->
                                        ExportsScreen(
                                            runId = state.export.runId,
                                            onRunIdChange = viewModel::updateExportRunId,
                                            isLoading = state.export.isLoading,
                                            statusMessage = state.export.statusMessage,
                                            errorMessage = state.export.errorMessage,
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                            onExportReport = { viewModel.exportRunReport(shareFile) },
                                            onExportAudit = { viewModel.exportAuditBundle(shareFile) },
                                        )

                                    AppScreen.TRACE ->
                                        TraceScreen(
                                            score = state.trace.score,
                                            alerts = state.trace.alerts,
                                            isLoading = state.trace.isLoading,
                                            statusMessage = state.trace.statusMessage,
                                            errorMessage = state.trace.errorMessage,
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack,
                                            onRefresh = viewModel::loadTraceSummary,
                                        )

                                    AppScreen.UNSUPPORTED ->
                                        UnsupportedScreen(
                                            message = state.unsupportedMessage ?: "Unsupported on this device tier.",
                                            canGoBack = canGoBack,
                                            onBack = viewModel::navigateBack
                                        )

                                    else -> Unit
                                }
                            }
                        }
                    }

                    state.statusMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(NgSpacing.Large),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            tonalElevation = 4.dp,
                        ) {
                            Text(
                                msg,
                                modifier = Modifier.padding(NgSpacing.Medium),
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onStart: () -> Unit,
    onOpen: (String) -> Unit,
    onPoll: () -> Unit,
) {
    val background = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.surface
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, 1200f)
    )
    val halo = Brush.radialGradient(
        colors = listOf(NgColors.Primary.copy(alpha = 0.18f), Color.Transparent),
        center = Offset(400f, 240f),
        radius = 520f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(halo)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(NgSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NgSpacing.Large),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("NeoGenesis RegenOps", style = MaterialTheme.typography.labelLarge, color = NgColors.Primary)
                Text("Precision Control Access", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Secure device authorization for mission-critical workflows.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NgCard(modifier = Modifier.widthIn(max = 520.dp)) {
                Column(
                    modifier = Modifier.padding(NgSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium),
                ) {
                    if (state.deviceAuthorization == null) {
                        Text("Authorize this terminal to begin operational control.")

                        if (state.statusMessage == "OIDC config missing") {
                            NgStatusChip(text = "Configuration Error", status = NgStatus.Error)
                            Text(
                                "OIDC_ISSUER or OIDC_CLIENT_ID is not set in build configuration.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            NgPrimaryButton(text = "Begin Authorization", onClick = onStart)
                        }
                    } else {
                        val device = state.deviceAuthorization
                        Text("User Code", style = MaterialTheme.typography.labelLarge)
                        Text(
                            device.userCode,
                            style = MaterialTheme.typography.displayMedium,
                            color = NgColors.Primary,
                        )

                        Text(
                            "1. Open the verification URL on another device.\n" +
                                    "2. Enter the code shown above.\n" +
                                    "3. Click 'Confirm Authorization' here.",
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        NgPrimaryButton(
                            text = "Open Verification URL",
                            onClick = { onOpen(device.verificationUriComplete ?: device.verificationUri) },
                        )

                        OutlinedButton(
                            onClick = onPoll,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Confirm Authorization")
                        }

                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                        Text(
                            "Polling for authorization...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.statusMessage != null && state.statusMessage != "OIDC config missing") {
                NgStatusChip(text = state.statusMessage!!, status = NgStatus.Warning)
            }
        }
    }
}

@Composable
private fun NgNavigationRail(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    state: RegenOpsUiState,
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.padding(vertical = NgSpacing.Large),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        navItems(state).forEach { item ->
            NavigationRailItem(
                selected = currentScreen == item.screen,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun NgBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    state: RegenOpsUiState,
) {
    NavigationBar {
        navItems(state).forEach { item ->
            NavigationBarItem(
                selected = currentScreen == item.screen,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

private data class NavItem(val screen: AppScreen, val label: String, val icon: ImageVector)

@Composable
private fun navItems(state: RegenOpsUiState) = buildList {
    val fallbackCaps =
        if (state.devicePolicy == null) setOf(com.neogenesis.platform.shared.domain.device.Capability.READ_ONLY_DASHBOARD)
        else state.devicePolicy.effectiveCapabilities
    val gate = CapabilityGate(fallbackCaps)
    if (gate.canAccess(AppScreen.PROTOCOLS)) add(NavItem(AppScreen.PROTOCOLS, "Protocols", Icons.Default.List))
    if (gate.canAccess(AppScreen.RUN_CONTROL)) add(NavItem(AppScreen.RUN_CONTROL, "Control", Icons.Default.PlayArrow))
    if ((state.selectedRunId != null || state.screen == AppScreen.LIVE_RUN) && gate.canAccess(AppScreen.LIVE_RUN)) {
        add(NavItem(AppScreen.LIVE_RUN, "Live", Icons.Default.Info))
    }
    if (state.traceModeEnabled && gate.canAccess(AppScreen.TRACE)) add(NavItem(AppScreen.TRACE, "Trace", Icons.Default.CheckCircle))
    if (gate.canAccess(AppScreen.EXPORTS)) add(NavItem(AppScreen.EXPORTS, "Exports", Icons.Default.KeyboardArrowDown))
    if (state.commercialModeEnabled && gate.canAccess(AppScreen.COMMERCIAL)) add(NavItem(AppScreen.COMMERCIAL, "Pipeline", Icons.Default.Star))
}

@Composable
private fun UnsupportedScreen(
    message: String,
    canGoBack: Boolean,
    onBack: () -> Unit
) {
    NgCard {
        Column(
            modifier = Modifier.padding(NgSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)
        ) {
            Text("Unsupported", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (canGoBack) {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Go Back")
                }
            }
        }
    }
}

object WindowSize {
    @Composable
    fun isDesktop(): Boolean = !isAndroid()
}
