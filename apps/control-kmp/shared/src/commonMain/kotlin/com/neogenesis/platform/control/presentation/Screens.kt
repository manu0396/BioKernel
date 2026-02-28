package com.neogenesis.platform.control.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.control.presentation.design.*
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.telemetry.TelemetryFrame

@Composable
fun ProtocolsScreen(
    protocols: List<Protocol>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Protocol) -> Unit,
    onRefresh: () -> Unit
) {
    val filtered = if (query.isBlank()) {
        protocols
    } else {
        protocols.filter { it.name.contains(query, ignoreCase = true) || (it.summary?.contains(query, ignoreCase = true) == true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Protocols", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }

        NgTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Search operational protocols"
        )

        if (filtered.isEmpty()) {
            NgEmptyState(
                title = "No Protocols Found",
                message = "Adjust your search or connect to a control node."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                items(filtered) { protocol ->
                    NgCard(onClick = { onSelect(protocol) }) {
                        Row(
                            modifier = Modifier.padding(NgSpacing.Medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(protocol.name, style = MaterialTheme.typography.titleMedium)
                                protocol.summary?.let { 
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            NgStatusChip("v${protocol.latestVersion?.version ?: "1"}", NgStatus.Info)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProtocolDetailScreen(
    protocol: Protocol?,
    selectedVersion: ProtocolVersion?,
    onBack: () -> Unit,
    onSelectVersion: (ProtocolVersion) -> Unit,
    onPublish: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Protocols") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Protocol Detail", style = MaterialTheme.typography.labelLarge)
        }

        if (protocol == null) {
            NgEmptyState(title = "No Protocol Selected", message = "Select a protocol from the list to view its configuration.")
        } else {
            Text(protocol.name, style = MaterialTheme.typography.headlineMedium)
            
            NgCard {
                Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(protocol.summary ?: "No summary provided.", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text("Version History", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Tiny)) {
                items(protocol.versions) { version ->
                    val isSelected = selectedVersion?.id == version.id
                    Surface(
                        onClick = { onSelectVersion(version) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(modifier = Modifier.padding(NgSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                            Text("v${version.version}", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.weight(1f))
                            if (version.published) {
                                NgStatusChip("PUBLISHED", NgStatus.Success)
                            } else {
                                NgStatusChip("DRAFT", NgStatus.Info)
                            }
                        }
                    }
                }
            }

            selectedVersion?.let { version ->
                NgCard {
                    Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                        Text("Payload Preview", style = MaterialTheme.typography.labelMedium)
                        Text(
                            version.payload.take(200) + if (version.payload.length > 200) "..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        )
                        
                        if (!version.published) {
                            Spacer(modifier = Modifier.height(NgSpacing.Small))
                            NgPrimaryButton(text = "Publish v${version.version}", onClick = onPublish)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RunControlScreen(
    protocols: List<Protocol>,
    selectedProtocol: Protocol?,
    selectedVersion: ProtocolVersion?,
    runs: List<Run>,
    demoModeEnabled: Boolean,
    simulatedRunEnabled: Boolean,
    onSimulatedRunToggle: (Boolean) -> Unit,
    onSelectProtocol: (Protocol) -> Unit,
    onSelectVersion: (ProtocolVersion) -> Unit,
    onStartRun: () -> Unit,
    onStartDemoRun: () -> Unit,
    onPauseRun: () -> Unit,
    onAbortRun: () -> Unit,
    onSelectRun: (String) -> Unit,
    onRefreshRuns: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Text("Run Control", style = MaterialTheme.typography.headlineSmall)

        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
                Text("Operational Configuration", style = MaterialTheme.typography.labelLarge)
                
                // Protocol Selection
                Text("Select Protocol", style = MaterialTheme.typography.labelSmall)
                // Simplified selection for professional feel
                Text(selectedProtocol?.name ?: "None selected", style = MaterialTheme.typography.titleMedium)
                
                // Version Selection
                Text("Select Version", style = MaterialTheme.typography.labelSmall)
                Text(selectedVersion?.version?.let { "v$it" } ?: "None selected", style = MaterialTheme.typography.titleMedium)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Simulated Run", style = MaterialTheme.typography.titleSmall)
                        Text("Run in Digital Twin mode", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = simulatedRunEnabled, onCheckedChange = onSimulatedRunToggle)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    NgPrimaryButton(
                        text = "Start Mission", 
                        onClick = onStartRun, 
                        modifier = Modifier.weight(1f),
                        enabled = selectedVersion != null
                    )
                    if (demoModeEnabled) {
                        OutlinedButton(onClick = onStartDemoRun, modifier = Modifier.height(48.dp)) {
                            Text("Demo")
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Active & Recent Missions", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onRefreshRuns) { Text("Refresh") }
        }

        if (runs.isEmpty()) {
            NgEmptyState(title = "No Missions", message = "Start a protocol to begin operational tracking.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                items(runs) { run ->
                    NgCard(onClick = { onSelectRun(run.id.value) }) {
                        Row(modifier = Modifier.padding(NgSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(run.id.value, style = MaterialTheme.typography.labelMedium)
                                Text("Status: ${run.status.name}", style = MaterialTheme.typography.bodySmall)
                            }
                            NgStatusChip(run.status.name, when(run.status.name) {
                                "RUNNING" -> NgStatus.Success
                                "PAUSED" -> NgStatus.Warning
                                "FAILED", "ABORTED" -> NgStatus.Error
                                else -> NgStatus.Info
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveRunScreen(
    runId: String?,
    runEvents: List<RunEvent>,
    telemetryFrames: List<TelemetryFrame>
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Text("Mission Live Control", style = MaterialTheme.typography.headlineSmall)

        if (runId == null) {
            NgEmptyState(title = "No Mission Selected", message = "Select a run from the control panel to view live telemetry.")
        } else {
            NgCard {
                Column(modifier = Modifier.padding(NgSpacing.Medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NgStatusChip("LIVE STREAM", NgStatus.Success)
                        Spacer(modifier = Modifier.width(NgSpacing.Small))
                        Text(runId, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.height(NgSpacing.Medium))
                    TelemetryChart(frames = telemetryFrames)
                }
            }

            Text("Event Timeline", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                items(runEvents) { event ->
                    NgCard {
                        Column(modifier = Modifier.padding(NgSpacing.Medium)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(event.eventType, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(event.createdAt.toString(), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(event.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommercialPipelineScreen(
    pipeline: CommercialPipeline,
    selected: CommercialOpportunity?,
    error: String?,
    onSelect: (CommercialOpportunity) -> Unit,
    onExport: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Commercial Pipeline", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                TextButton(onClick = onRefresh) { Text("Refresh") }
                TextButton(onClick = onExport) { Text("Export CSV") }
            }
        }

        if (error != null) NgStatusChip(error, NgStatus.Error)

        if (pipeline.stages.isEmpty()) {
            NgEmptyState(title = "No Opportunities", message = "Connect to CRM node to sync pipeline.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Large)) {
                pipeline.stages.forEach { (stage, opportunities) ->
                    item {
                        Text(stage.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    items(opportunities) { opportunity ->
                        NgCard(onClick = { onSelect(opportunity) }) {
                            Row(modifier = Modifier.padding(NgSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(opportunity.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Probability: ${opportunity.probability}%", style = MaterialTheme.typography.bodySmall)
                                }
                                Text("€${opportunity.expectedRevenueEur}", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        selected?.let { opp ->
            AlertDialog(
                onDismissRequest = { /* Handle dismiss */ },
                title = { Text(opp.name) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                        Text("Stage: ${opp.stage}")
                        Text("Expected Revenue: €${opp.expectedRevenueEur}")
                        Text("Probability: ${opp.probability}%")
                        Text("LOI Signed: ${if (opp.loiSigned) "Yes" else "No"}")
                        if (opp.notes.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = NgSpacing.Small))
                            Text(opp.notes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { /* Handle close */ }) { Text("Close") } }
            )
        }
    }
}

@Composable
fun ExportsScreen(
    runId: String,
    onRunIdChange: (String) -> Unit,
    isLoading: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onExportReport: () -> Unit,
    onExportAudit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Text("Evidence & Audit Exports", style = MaterialTheme.typography.headlineSmall)

        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
                Text("Select Mission", style = MaterialTheme.typography.labelLarge)
                NgTextField(
                    value = runId,
                    onValueChange = onRunIdChange,
                    label = "Run ID"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    NgPrimaryButton(
                        text = "Export Report", 
                        onClick = onExportReport,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onExportAudit,
                        modifier = Modifier.height(48.dp).weight(1f),
                        enabled = !isLoading && runId.isNotBlank()
                    ) {
                        Text("Audit Bundle")
                    }
                }
            }
        }

        if (statusMessage != null) NgStatusChip(statusMessage, NgStatus.Info)
        if (errorMessage != null) NgStatusChip(errorMessage, NgStatus.Error)

        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                Text("Export Hardening", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Audit bundles contain SHA-256 manifest integrity checks and are cryptographically linked to the BioKernel chain of evidence.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TraceScreen(
    score: Int?,
    alerts: List<DriftAlert>,
    isLoading: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Operational Trace", style = MaterialTheme.typography.headlineSmall)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRefresh) { Text("Refresh") }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
            NgMetricTile(
                label = "Integrity Score", 
                value = "${score ?: "--"}/100", 
                modifier = Modifier.weight(1f),
                trend = if ((score ?: 0) > 90) "Optimal" else "Attention Required"
            )
        }

        if (statusMessage != null) NgStatusChip(statusMessage, NgStatus.Info)
        if (errorMessage != null) NgStatusChip(errorMessage, NgStatus.Error)

        Text("Active Drift Alerts", style = MaterialTheme.typography.titleMedium)

        if (alerts.isEmpty()) {
            NgEmptyState(title = "No Integrity Alerts", message = "System status is nominal. No parameter drift detected.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                items(alerts) { alert ->
                    NgCard {
                        Row(modifier = Modifier.padding(NgSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.title, style = MaterialTheme.typography.titleMedium)
                                Text(alert.message, style = MaterialTheme.typography.bodySmall)
                            }
                            NgStatusChip(alert.severity, when(alert.severity.lowercase()) {
                                "high" -> NgStatus.Error
                                "medium" -> NgStatus.Warning
                                else -> NgStatus.Info
                            })
                        }
                    }
                }
            }
        }
    }
}
