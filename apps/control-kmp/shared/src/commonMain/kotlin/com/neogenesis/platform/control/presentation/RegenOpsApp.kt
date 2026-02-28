package com.neogenesis.platform.control.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.control.presentation.design.*

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

    NgTheme {
        val isDesktop = WindowSize.isDesktop()
        
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
            }
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
                        }
                    ) { screen ->
                        Column(
                            modifier = Modifier.fillMaxSize().padding(NgSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)
                        ) {
                            if (state.errorBanner != null) {
                                NgStatusChip(text = state.errorBanner!!, status = NgStatus.Error)
                            }

                            // Feature Badges
                            Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                                if (state.traceModeEnabled) NgStatusChip("TRACE ENABLED", NgStatus.Success)
                                if (state.demoModeEnabled) NgStatusChip("DEMO MODE", NgStatus.Info)
                            }

                            if (!state.auth.isAuthenticated) {
                                AuthScreen(
                                    state = state.auth,
                                    onStart = { viewModel.beginDeviceAuth { } },
                                    onOpen = openExternalUrl,
                                    onPoll = { viewModel.pollDeviceAuth() }
                                )
                            } else {
                                when (screen) {
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
                                        onExportReport = { viewModel.exportRunReport(shareFile) },
                                        onExportAudit = { viewModel.exportAuditBundle(shareFile) }
                                    )
                                    AppScreen.TRACE -> TraceScreen(
                                        score = state.trace.score,
                                        alerts = state.trace.alerts,
                                        isLoading = state.trace.isLoading,
                                        statusMessage = state.trace.statusMessage,
                                        errorMessage = state.trace.errorMessage,
                                        onRefresh = { viewModel.loadTraceSummary() }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }

                    // Global Status Overlay
                    state.statusMessage?.let {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(NgSpacing.Large),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            tonalElevation = 4.dp
                        ) {
                            Text(it, modifier = Modifier.padding(NgSpacing.Medium), color = MaterialTheme.colorScheme.inverseOnSurface)
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
    onPoll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(NgSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NgSpacing.Large)
    ) {
        Text("Precision Control Access", style = MaterialTheme.typography.displaySmall)
        
        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
                if (state.deviceAuthorization == null) {
                    Text("Authorize this terminal to begin operational control.")
                    
                    if (state.statusMessage == "OIDC config missing") {
                        NgStatusChip("Configuration Error", NgStatus.Error)
                        Text("OIDC_ISSUER or OIDC_CLIENT_ID is not set in build configuration.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        NgPrimaryButton(text = "Begin Authorization", onClick = onStart)
                    }
                } else {
                    val device = state.deviceAuthorization
                    Text("User Code", style = MaterialTheme.typography.labelLarge)
                    Text(
                        device.userCode, 
                        style = MaterialTheme.typography.displayMedium, 
                        color = NgColors.Primary
                    )
                    
                    Text("1. Open the verification URL on another device.\n2. Enter the code shown above.\n3. Click 'Confirm Authorization' here.", style = MaterialTheme.typography.bodyMedium)

                    NgPrimaryButton(text = "Open Verification URL", onClick = { onOpen(device.verificationUriComplete ?: device.verificationUri) })
                    
                    OutlinedButton(onClick = onPoll, modifier = Modifier.fillMaxWidth()) {
                        Text("Confirm Authorization")
                    }
                    
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                    Text("Polling for authorization...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        if (state.statusMessage != null && state.statusMessage != "OIDC config missing") {
            NgStatusChip(state.statusMessage, NgStatus.Warning)
        }
    }
}

@Composable
private fun NgNavigationRail(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    state: RegenOpsUiState
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.padding(vertical = NgSpacing.Large),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        navItems(state).forEach { item ->
            NavigationRailItem(
                selected = currentScreen == item.screen,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun NgBottomNavigation(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    state: RegenOpsUiState
) {
    NavigationBar {
        navItems(state).forEach { item ->
            NavigationBarItem(
                selected = currentScreen == item.screen,
                onClick = { onNavigate(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

private data class NavItem(val screen: AppScreen, val label: String, val icon: ImageVector)

@Composable
private fun navItems(state: RegenOpsUiState) = buildList {
    add(NavItem(AppScreen.PROTOCOLS, "Protocols", Icons.Default.List))
    add(NavItem(AppScreen.RUN_CONTROL, "Control", Icons.Default.PlayArrow))
    if (state.selectedRunId != null || state.screen == AppScreen.LIVE_RUN) {
        add(NavItem(AppScreen.LIVE_RUN, "Live", Icons.Default.Info))
    }
    if (state.traceModeEnabled) {
        add(NavItem(AppScreen.TRACE, "Trace", Icons.Default.CheckCircle))
    }
    add(NavItem(AppScreen.EXPORTS, "Exports", Icons.Default.KeyboardArrowDown))
    if (state.commercialModeEnabled) {
        add(NavItem(AppScreen.COMMERCIAL, "Pipeline", Icons.Default.Star))
    }
}

// Simple helper for responsive layout
object WindowSize {
    @Composable
    fun isDesktop(): Boolean {
        return !isAndroid()
    }
}
