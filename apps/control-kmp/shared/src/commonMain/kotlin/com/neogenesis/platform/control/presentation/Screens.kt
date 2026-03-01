package com.neogenesis.platform.control.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.control.presentation.design.NgCard
import com.neogenesis.platform.control.presentation.design.NgEmptyState
import com.neogenesis.platform.control.presentation.design.NgMetricTile
import com.neogenesis.platform.control.presentation.design.NgPrimaryButton
import com.neogenesis.platform.control.presentation.design.NgSpacing
import com.neogenesis.platform.control.presentation.design.NgStatus
import com.neogenesis.platform.control.presentation.design.NgStatusChip
import com.neogenesis.platform.control.presentation.design.NgTextField
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// -------------------------------------------------------------------------------------
// Protocols
// -------------------------------------------------------------------------------------
@Composable
fun ProtocolsScreen(
    protocols: List<Protocol>,
    query: String,
    onQueryChange: (String) -> Unit,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onSelect: (Protocol) -> Unit,
    onRefresh: () -> Unit,
) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val filtered =
        if (query.isBlank()) {
            protocols
        } else {
            protocols.filter { protocol ->
                val summary = protocol.summary ?: ""
                val latest = protocol.latestVersion
                val latestAuthor = latest?.author ?: ""
                protocol.name.contains(query, ignoreCase = true) ||
                        protocol.id.value.contains(query, ignoreCase = true) ||
                        summary.contains(query, ignoreCase = true) ||
                        latestAuthor.contains(query, ignoreCase = true)
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canGoBack) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Spacer(modifier = Modifier.width(NgSpacing.Small))
                }
                Text("Protocols", style = MaterialTheme.typography.headlineSmall)
            }
            TextButton(onClick = onRefresh) { Text("Refresh") }
        }

        NgTextField(
            value = query,
            onValueChange = onQueryChange,
            label = "Search operational protocols",
        )

        if (filtered.isEmpty()) {
            NgEmptyState(
                title = "No Protocols Found",
                message = "Adjust your search or connect to a control node.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                items(filtered) { protocol ->
                    val latest = protocol.latestVersion
                    val latestVersionLabel = latest?.version?.let { "v$it" } ?: "no versions"
                    val isPublished = latest?.published == true
                    val publishedCount = protocol.versions.count { it.published }
                    val summary = protocol.summary ?: ""

                    val isExpanded = expanded[protocol.id.value] == true
                    val outcome = protocol.lastOutcome ?: "UNKNOWN"
                    val outcomeStatus =
                        when (outcome.uppercase()) {
                            "SUCCESS", "COMPLETED" -> NgStatus.Success
                            "WARNING" -> NgStatus.Warning
                            "FAILED", "ABORTED" -> NgStatus.Error
                            else -> NgStatus.Info
                        }

                    NgCard(onClick = { expanded[protocol.id.value] = !isExpanded }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(NgSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(NgSpacing.Small),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(protocol.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            protocol.id.value,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        NgStatusChip(
                                            text = if (isPublished) "Published" else "Draft",
                                            status = if (isPublished) NgStatus.Success else NgStatus.Info,
                                        )
                                        NgStatusChip(text = outcome, status = outcomeStatus)
                                    }
                                    ProtocolBadge(text = latestVersionLabel)
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isExpanded) {
                                if (summary.isNotBlank()) {
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ProtocolBadge(text = "${protocol.versions.size} versions")
                                    if (publishedCount > 0) ProtocolBadge(text = "$publishedCount published")
                                    latest?.let { v ->
                                        if (v.author.isNotBlank()) ProtocolBadge(text = "by ${v.author}")
                                        ProtocolBadge(text = formatInstant(v.createdAt))
                                    }
                                }
                            } else {
                                if (summary.isNotBlank()) {
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                protocol.resultSummary?.let {
                                    Text(
                                        text = "Result: $it",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (protocol.resultMetrics.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        protocol.resultMetrics.entries.take(4).forEach { (label, value) ->
                                            NgMetricTile(label = label, value = value, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ProtocolBadge(text = "${protocol.versions.size} versions")
                                    if (publishedCount > 0) ProtocolBadge(text = "$publishedCount published")
                                    latest?.let { v ->
                                        ProtocolBadge(text = "Latest v${v.version}")
                                        if (v.author.isNotBlank()) ProtocolBadge(text = "by ${v.author}")
                                        ProtocolBadge(text = formatInstant(v.createdAt))
                                    }
                                }
                                TextButton(onClick = { onSelect(protocol) }) { Text("Open Protocol") }
                            }
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
    canGoBack: Boolean = true,
    onBack: () -> Unit,
    onSelectVersion: (ProtocolVersion) -> Unit,
    onPublish: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (canGoBack) TextButton(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(NgSpacing.Small))
            Text(protocol?.name ?: "Protocol", style = MaterialTheme.typography.headlineSmall)
        }

        if (protocol == null) {
            NgEmptyState(
                title = "No Protocol Selected",
                message = "Select a protocol from the list to view its configuration.",
            )
            return
        }

        NgCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(NgSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(NgSpacing.Small),
            ) {
                Text(
                    protocol.id.value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val summary = protocol.summary ?: ""
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                protocol.resultSummary?.let {
                    Text("Result Overview", style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }

                if (protocol.resultMetrics.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        protocol.resultMetrics.entries.take(4).forEach { (label, value) ->
                            NgMetricTile(label = label, value = value, modifier = Modifier.weight(1f))
                        }
                    }
                }

                val latest = protocol.latestVersion
                val publishedCount = protocol.versions.count { it.published }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProtocolBadge(text = "${protocol.versions.size} versions")
                    if (publishedCount > 0) ProtocolBadge(text = "$publishedCount published")
                    latest?.let { v ->
                        ProtocolBadge(text = "Latest v${v.version}")
                        ProtocolBadge(text = formatInstant(v.createdAt))
                        if (v.author.isNotBlank()) ProtocolBadge(text = "by ${v.author}")
                    }
                }
            }
        }

        Text("Versions", style = MaterialTheme.typography.titleMedium)

        if (protocol.versions.isEmpty()) {
            NgEmptyState(title = "No protocol versions", message = "This protocol has no versions yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                items(protocol.versions.sortedByDescending { it.createdAt }) { version ->
                    val isSelected = selectedVersion?.id == version.id
                    val isPublished = version.published

                    NgCard(onClick = { onSelectVersion(version) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(NgSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "v${version.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                NgStatusChip(
                                    text = if (isPublished) "Published" else "Draft",
                                    status = if (isPublished) NgStatus.Success else NgStatus.Info,
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProtocolBadge(text = formatInstant(version.createdAt))
                                if (version.author.isNotBlank()) ProtocolBadge(text = "by ${version.author}")
                                if (isSelected) ProtocolBadge(text = "selected")
                            }

                            Text(
                                text = "Id: ${version.id.value}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        selectedVersion?.let { version ->
            Text("Selected Version", style = MaterialTheme.typography.titleMedium)

            NgCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(NgSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(NgSpacing.Small),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NgStatusChip(
                            text = if (version.published) "Published" else "Draft",
                            status = if (version.published) NgStatus.Success else NgStatus.Info,
                        )
                        ProtocolBadge(text = "v${version.version}")
                        ProtocolBadge(text = formatInstant(version.createdAt))
                        if (version.author.isNotBlank()) ProtocolBadge(text = "by ${version.author}")
                    }

                    Text("Payload", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = version.payload,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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

// -------------------------------------------------------------------------------------
// Run Control + Simulation Config + Protocol/Version pickers
// -------------------------------------------------------------------------------------
@Composable
fun RunControlScreen(
    protocols: List<Protocol>,
    selectedProtocol: Protocol?,
    selectedVersion: ProtocolVersion?,
    runs: List<Run>,
    demoModeEnabled: Boolean,
    simulatedRunEnabled: Boolean,
    isStartingRun: Boolean,
    onSimulatedRunToggle: (Boolean) -> Unit,
    onSelectProtocol: (Protocol) -> Unit,
    onSelectVersion: (ProtocolVersion) -> Unit,
    onStartRun: () -> Unit,
    onStartSimulatedRun: (SimulationConfig) -> Unit,
    onStartDemoRun: () -> Unit,
    onPauseRun: () -> Unit,
    onAbortRun: () -> Unit,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onSelectRun: (String) -> Unit,
    onRefreshRuns: () -> Unit,
) {
    var showProtocolPicker by remember { mutableStateOf(false) }
    var showVersionPicker by remember { mutableStateOf(false) }
    var showSimConfig by remember { mutableStateOf(false) }

    var durationMinutes by remember { mutableStateOf("30") }

    val dialogProperties = if (WindowSize.isDesktop()) {
        DialogProperties(usePlatformDefaultWidth = false)
    } else {
        DialogProperties()
    }
    val dialogModifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)

    var tickMillis by remember { mutableStateOf("250") }
    var speedFactor by remember { mutableStateOf("1.0") }
    var operatorName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    if (showProtocolPicker) {
        AlertDialog(
            onDismissRequest = { showProtocolPicker = false },
            properties = dialogProperties,
            modifier = dialogModifier,
            title = { Text("Select Protocol") },
            text = {
                if (protocols.isEmpty()) {
                    Text("No protocols available.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(protocols) { p ->
                            TextButton(
                                onClick = {
                                    onSelectProtocol(p)
                                    showProtocolPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(p.name) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProtocolPicker = false }) { Text("Close") } },
        )
    }

    if (showVersionPicker) {
        val versions = selectedProtocol?.versions.orEmpty().sortedByDescending { it.createdAt }
        AlertDialog(
            onDismissRequest = { showVersionPicker = false },
            properties = dialogProperties,
            modifier = dialogModifier,
            title = { Text("Select Version") },
            text = {
                if (selectedProtocol == null) {
                    Text("Select a protocol first.")
                } else if (versions.isEmpty()) {
                    Text("No versions available.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(versions) { v ->
                            TextButton(
                                onClick = {
                                    onSelectVersion(v)
                                    showVersionPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val tag = if (v.published) "Published" else "Draft"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("v${v.version}")
                                    Text(tag, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showVersionPicker = false }) { Text("Close") } },
        )
    }

    if (showSimConfig) {
        AlertDialog(
            onDismissRequest = { showSimConfig = false },
            properties = dialogProperties,
            modifier = dialogModifier,
            title = { Text("Simulation Settings") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    NgTextField(value = durationMinutes, onValueChange = { durationMinutes = it.filter(Char::isDigit) }, label = "Duration (minutes)")
                    NgTextField(value = tickMillis, onValueChange = { tickMillis = it.filter(Char::isDigit) }, label = "Tick (ms)")
                    NgTextField(value = speedFactor, onValueChange = { speedFactor = it }, label = "Speed factor (e.g. 1.0, 2.0)")
                    NgTextField(value = operatorName, onValueChange = { operatorName = it }, label = "Operator")
                    NgTextField(value = notes, onValueChange = { notes = it }, label = "Notes")

                    HorizontalDivider()
                    Text(
                        "These values are forwarded to the ViewModel. Wire them to backend when API supports it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSimConfig = false
                        val cfg = SimulationConfig(
                            durationMinutes = durationMinutes.toIntOrNull() ?: 30,
                            tickMillis = tickMillis.toIntOrNull() ?: 250,
                            speedFactor = speedFactor.toDoubleOrNull() ?: 1.0,
                            operatorName = operatorName,
                            notes = notes,
                        )
                        onStartSimulatedRun(cfg)
                    },
                ) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showSimConfig = false }) { Text("Cancel") } },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (canGoBack) TextButton(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(NgSpacing.Small))
            Text("Run Control", style = MaterialTheme.typography.headlineSmall)
        }

        NgCard {
            Column(
                modifier = Modifier.padding(NgSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium),
            ) {
                Text("Operational Configuration", style = MaterialTheme.typography.labelLarge)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Protocol", style = MaterialTheme.typography.labelSmall)
                        Text(selectedProtocol?.name ?: "None selected", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(onClick = { showProtocolPicker = true }, modifier = Modifier.height(48.dp)) { Text("Change") }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Version", style = MaterialTheme.typography.labelSmall)
                        Text(selectedVersion?.version?.let { "v$it" } ?: "None selected", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(
                        onClick = { showVersionPicker = true },
                        modifier = Modifier.height(48.dp),
                        enabled = selectedProtocol != null,
                    ) { Text("Change") }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Simulated Run", style = MaterialTheme.typography.titleSmall)
                        Text("Run in Digital Twin mode", style = MaterialTheme.typography.labelSmall)
                    }
                    Switch(checked = simulatedRunEnabled, onCheckedChange = onSimulatedRunToggle)
                }

                if (simulatedRunEnabled) {
                    OutlinedButton(
                        onClick = { showSimConfig = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = selectedVersion != null,
                    ) { Text("Configure Simulation") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    NgPrimaryButton(
                        text = if (simulatedRunEnabled) "Start Simulation" else "Start Mission",
                        onClick = { if (simulatedRunEnabled) showSimConfig = true else onStartRun() },
                        modifier = Modifier.weight(1f),
                        isLoading = isStartingRun,
                        enabled = selectedVersion != null,
                    )

                    if (demoModeEnabled) {
                        OutlinedButton(onClick = onStartDemoRun, modifier = Modifier.height(48.dp)) { Text("Demo") }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    OutlinedButton(onClick = onPauseRun, modifier = Modifier.height(48.dp)) { Text("Pause") }
                    OutlinedButton(onClick = onAbortRun, modifier = Modifier.height(48.dp)) { Text("Stop") }
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
                            val status =
                                when (run.status.name) {
                                    "RUNNING" -> NgStatus.Success
                                    "PAUSED" -> NgStatus.Warning
                                    "FAILED", "ABORTED" -> NgStatus.Error
                                    else -> NgStatus.Info
                                }
                            NgStatusChip(text = run.status.name, status = status)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Live Run + Pause/Stop + Download Report (FIXED: onDownloadReport name)
// -------------------------------------------------------------------------------------
@Composable
fun LiveRunScreen(
    runId: String?,
    runEvents: List<RunEvent>,
    telemetryFrames: List<TelemetryFrame>,
    streamStatus: String? = null,
    onPause: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onDownloadReport: (() -> Unit)? = null,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    val isFinished =
        runEvents.any { e ->
            e.eventType.contains("completed", ignoreCase = true) ||
                    e.eventType.contains("finished", ignoreCase = true) ||
                    e.eventType.contains("aborted", ignoreCase = true) ||
                    e.eventType.contains("failed", ignoreCase = true)
        }

    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canGoBack) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Spacer(modifier = Modifier.width(NgSpacing.Small))
                }
                Text("Mission Live Control", style = MaterialTheme.typography.headlineSmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                if (onPause != null) OutlinedButton(onClick = onPause, modifier = Modifier.height(48.dp)) { Text("Pause") }
                if (onStop != null) OutlinedButton(onClick = onStop, modifier = Modifier.height(48.dp)) { Text("Stop") }
            }
        }

        if (runId == null) {
            NgEmptyState(title = "No Mission Selected", message = "Select a run to view live telemetry.")
            return
        }

        streamStatus?.let { NgStatusChip(text = it, status = NgStatus.Info) }

        // Show download entry whenever export is supported (even before finished).
        if (onDownloadReport != null) {
            NgCard {
                Column(
                    modifier = Modifier.padding(NgSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(NgSpacing.Small),
                ) {
                    Text(if (isFinished) "Mission finished" else "Mission report", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (isFinished) "Download the full report." else "You can download a partial report while running.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NgPrimaryButton(text = "Download Report", onClick = onDownloadReport)
                }
            }
        }

        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LivePulseBadge()
                    NgStatusChip(text = "LIVE STREAM", status = NgStatus.Success)
                    Spacer(modifier = Modifier.width(NgSpacing.Small))
                    Text(runId, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(NgSpacing.Medium))
                TelemetryChart(frames = telemetryFrames)
            }
        }

        Text("Event Timeline", style = MaterialTheme.typography.titleMedium)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
            items(runEvents) { event ->
                NgCard {
                    Column(modifier = Modifier.padding(NgSpacing.Medium)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val status = when {
                                    event.eventType.contains("failed", true) || event.eventType.contains("aborted", true) -> NgStatus.Error
                                    event.eventType.contains("paused", true) -> NgStatus.Warning
                                    event.eventType.contains("started", true) || event.eventType.contains("completed", true) -> NgStatus.Success
                                    else -> NgStatus.Info
                                }
                                NgStatusChip(text = event.eventType, status = status)
                            }
                            Text(event.createdAt.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(event.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePulseBadge() {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val alpha = transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )
    androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
        drawCircle(color = NgColors.Success.copy(alpha = alpha.value))
    }
}

// -------------------------------------------------------------------------------------
// Commercial / Exports / Trace (back buttons added)
// -------------------------------------------------------------------------------------
@Composable
fun CommercialPipelineScreen(
    pipeline: CommercialPipeline,
    selected: CommercialOpportunity?,
    error: String?,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onSelect: (CommercialOpportunity) -> Unit,
    onDismissSelected: () -> Unit,
    onExport: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canGoBack) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Spacer(modifier = Modifier.width(NgSpacing.Small))
                }
                Text("Commercial Pipeline", style = MaterialTheme.typography.headlineSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                TextButton(onClick = onRefresh) { Text("Refresh") }
                TextButton(onClick = onExport) { Text("Export CSV") }
            }
        }

        if (error != null) NgStatusChip(text = error, status = NgStatus.Error)

        if (pipeline.stages.isEmpty()) {
            NgEmptyState(title = "No Opportunities", message = "Connect to CRM node to sync pipeline.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
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
                onDismissRequest = onDismissSelected,
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
                confirmButton = { TextButton(onClick = onDismissSelected) { Text("Close") } },
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
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onExportReport: () -> Unit,
    onExportAudit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (canGoBack) TextButton(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(NgSpacing.Small))
            Text("Evidence & Audit Exports", style = MaterialTheme.typography.headlineSmall)
        }

        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
                Text("Select Mission", style = MaterialTheme.typography.labelLarge)
                NgTextField(value = runId, onValueChange = onRunIdChange, label = "Run ID")

                Row(horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                    NgPrimaryButton(
                        text = "Export Report",
                        onClick = onExportReport,
                        isLoading = isLoading,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = onExportAudit,
                        modifier = Modifier.height(48.dp).weight(1f),
                        enabled = !isLoading && runId.isNotBlank(),
                    ) { Text("Audit Bundle") }
                }
            }
        }

        if (statusMessage != null) NgStatusChip(text = statusMessage, status = NgStatus.Info)
        if (errorMessage != null) NgStatusChip(text = errorMessage, status = NgStatus.Error)

        NgCard {
            Column(modifier = Modifier.padding(NgSpacing.Medium), verticalArrangement = Arrangement.spacedBy(NgSpacing.Small)) {
                Text("Export Hardening", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Audit bundles contain SHA-256 manifest integrity checks and are cryptographically linked to the BioKernel chain of evidence.",
                    style = MaterialTheme.typography.bodySmall,
                )
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
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NgSpacing.Medium)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canGoBack) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Spacer(modifier = Modifier.width(NgSpacing.Small))
                }
                Text("Operational Trace", style = MaterialTheme.typography.headlineSmall)
            }
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
                trend = if ((score ?: 0) > 90) "Optimal" else "Attention Required",
            )
        }

        if (statusMessage != null) NgStatusChip(text = statusMessage, status = NgStatus.Info)
        if (errorMessage != null) NgStatusChip(text = errorMessage, status = NgStatus.Error)

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
                            val status =
                                when (alert.severity.lowercase()) {
                                    "high" -> NgStatus.Error
                                    "medium" -> NgStatus.Warning
                                    else -> NgStatus.Info
                                }
                            NgStatusChip(text = alert.severity, status = status)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------------------
@Composable
private fun ProtocolBadge(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun formatInstant(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    fun two(n: Int) = n.toString().padStart(2, '0')
    return buildString {
        append(dt.year); append("-"); append(two(dt.monthNumber)); append("-"); append(two(dt.dayOfMonth))
        append(" "); append(two(dt.hour)); append(":"); append(two(dt.minute))
    }
}

