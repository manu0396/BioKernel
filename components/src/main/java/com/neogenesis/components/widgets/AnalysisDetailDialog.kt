package com.neogenesis.components.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neogenesis.domain.model.RetinaAnalysis

@Composable
fun AnalysisDetailDialog(record: RetinaAnalysis, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Análisis: ${record.id}", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hash: ${record.rawHash}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Notas de Laboratorio:", style = MaterialTheme.typography.labelLarge)
                Text(record.notes, style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun EmptyStatePlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No hay telemetría disponible", style = MaterialTheme.typography.bodyLarge)
        Text("Pulsa refrescar para sincronizar", style = MaterialTheme.typography.bodySmall)
    }
}


