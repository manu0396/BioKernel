package com.neogenesis.platform.control.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.shared.telemetry.TelemetryFrame

@Composable
fun TelemetryChart(frames: List<TelemetryFrame>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Telemetry", style = MaterialTheme.typography.titleSmall)
        if (frames.isEmpty()) {
            Text("No telemetry yet.")
            return
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val pressure = frames.map { it.pressure.kpa }
            val flow = frames.map { it.flowRate.microlitersPerSecond }
            val maxPressure = pressure.maxOrNull() ?: 1.0
            val minPressure = pressure.minOrNull() ?: 0.0
            val maxFlow = flow.maxOrNull() ?: 1.0
            val minFlow = flow.minOrNull() ?: 0.0

            fun mapY(value: Double, min: Double, max: Double): Float {
                val normalized = if (max == min) 0.5 else (value - min) / (max - min)
                return (size.height * (1f - normalized.toFloat()))
            }

            val stepX = if (frames.size <= 1) 0f else size.width / (frames.size - 1)
            for (i in 1 until frames.size) {
                val x1 = stepX * (i - 1)
                val x2 = stepX * i
                drawLine(
                    color = Color(0xFF1565C0),
                    start = Offset(x1, mapY(pressure[i - 1], minPressure, maxPressure)),
                    end = Offset(x2, mapY(pressure[i], minPressure, maxPressure)),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color(0xFF2E7D32),
                    start = Offset(x1, mapY(flow[i - 1], minFlow, maxFlow)),
                    end = Offset(x2, mapY(flow[i], minFlow, maxFlow)),
                    strokeWidth = 3f
                )
            }
        }
        Text("Pressure (blue) vs Flow (green)")
    }
}
