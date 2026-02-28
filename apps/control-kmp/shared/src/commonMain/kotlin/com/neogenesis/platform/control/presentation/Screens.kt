package com.neogenesis.platform.control.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        protocols.filter { it.name.contains(query, ignoreCase = true) || it.summary.contains(query, ignoreCase = true) }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Protocols", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onRefresh) { Text("Refresh") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Search protocols") },
                modifier = Modifier.fillMaxWidth()
            )
            if (filtered.isEmpty()) {
                Text("No protocols available.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(filtered) { protocol ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelect(protocol) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(protocol.name, fontWeight = FontWeight.SemiBold)
                                Text(protocol.summary)
                                val versionLabel = protocol.latestVersion?.version ?: "n/a"
                                Text("Latest: v$versionLabel - Versions: ${'$'}{protocol.versions.size}")
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
    onBack: () -> Unit,
    onSelectVersion: (ProtocolVersion) -> Unit,
    onPublish: (ProtocolVersion) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Protocol Detail", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onBack) { Text("Back") }
            }
            if (protocol == null) {
                Text("Select a protocol to view details.")
            } else {
                Text(protocol.name, style = MaterialTheme.typography.titleMedium)
                Text(protocol.summary)
                Text("Versions", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    items(protocol.versions) { version ->
                        val selected = selectedVersion?.id?.value == version.id.value
                        val suffix = if (version.published) " (published)" else ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectVersion(version) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("v${'$'}{version.version}$suffix")
                            if (selected) {
                                Text("selected", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                val previous = protocol.versions.firstOrNull { it.id.value != selectedVersion?.id?.value }
                val diffSummary = buildVersionDiff(selectedVersion, previous)
                Text("Diff (latest vs previous)", style = MaterialTheme.typography.titleSmall)
                Text(diffSummary.ifBlank { "No differences detected." })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { selectedVersion?.let(onPublish) },
                        enabled = selectedVersion != null && selectedVersion.published.not()
                    ) {
                        Text("Publish version")
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
    onSelectProtocol: (Protocol) -> Unit,
    onSelectVersion: (ProtocolVersion) -> Unit,
    onStartRun: () -> Unit,
    onPauseRun: () -> Unit,
    onAbortRun: () -> Unit,
    onSelectRun: (String) -> Unit,
    onRefreshRuns: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Run Control", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onRefreshRuns) { Text("Refresh") }
            }
            Text("Protocol", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                items(protocols) { protocol ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProtocol(protocol) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(protocol.name)
                        if (protocol.id == selectedProtocol?.id) {
                            Text("selected", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Text("Version", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                items(selectedProtocol?.versions ?: emptyList()) { version ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVersion(version) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("v${'$'}{version.version}")
                        if (version.id == selectedVersion?.id) {
                            Text("selected", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartRun) { Text("Start") }
                Button(onClick = onPauseRun, enabled = runs.isNotEmpty()) { Text("Pause") }
                Button(onClick = onAbortRun, enabled = runs.isNotEmpty()) { Text("Abort") }
            }
            Text("Recent Runs", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                items(runs) { run ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRun(run.id.value) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(run.id.value)
                        Text(run.status.name)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Live Run", style = MaterialTheme.typography.titleLarge)
            if (runId == null) {
                Text("Select a run to view telemetry.")
            } else {
                Text("Run ID: ${'$'}runId")
                TelemetryChart(frames = telemetryFrames)
                Text("Event Timeline", style = MaterialTheme.typography.titleSmall)
                LazyColumn(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    items(runEvents) { event ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${'$'}{event.eventType} - ${'$'}{event.createdAt}")
                            Text(event.message)
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Commercial Pipeline", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRefresh) { Text("Refresh") }
                    Button(onClick = onExport) { Text("Export CSV") }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val stages = pipeline.stages
            if (stages.isEmpty()) {
                Text("No opportunities available.")
            } else {
                stages.forEach { (stage, items) ->
                    Text(stage, style = MaterialTheme.typography.titleSmall)
                    items.forEach { opportunity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(opportunity) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(opportunity.name)
                            Text("EUR ${'$'}{opportunity.expectedRevenueEur}")
                        }
                    }
                }
                selected?.let { opp ->
                    val loiLabel = if (opp.loiSigned) "Yes" else "No"
                    Text("Detail", style = MaterialTheme.typography.titleSmall)
                    Text("Name: ${'$'}{opp.name}")
                    Text("Stage: ${'$'}{opp.stage}")
                    Text("Expected EUR: ${'$'}{opp.expectedRevenueEur}")
                    Text("Probability: ${'$'}{opp.probability}%")
                    Text("LOI Signed: $loiLabel")
                    if (opp.notes.isNotBlank()) {
                        Text("Notes: ${'$'}{opp.notes}")
                    }
                }
            }
        }
    }
}

private fun buildVersionDiff(current: ProtocolVersion?, previous: ProtocolVersion?): String {
    if (current == null || previous == null) return ""
    val currentLines = current.payload.lines()
    val previousLines = previous.payload.lines()
    val max = maxOf(currentLines.size, previousLines.size)
    val diffs = buildList {
        for (i in 0 until max) {
            val left = previousLines.getOrNull(i)
            val right = currentLines.getOrNull(i)
            if (left != right) {
                if (left != null) add("- ${'$'}left")
                if (right != null) add("+ ${'$'}right")
            }
        }
    }
    return diffs.joinToString("\n")
}
