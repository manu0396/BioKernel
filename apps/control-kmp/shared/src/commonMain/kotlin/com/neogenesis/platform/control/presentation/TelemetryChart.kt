package com.neogenesis.platform.control.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.neogenesis.platform.control.presentation.design.NgColors
import com.neogenesis.platform.control.presentation.design.NgMetricTile
import com.neogenesis.platform.control.presentation.design.NgSpacing
import com.neogenesis.platform.shared.telemetry.TelemetryFrame

@Composable
fun TelemetryChart(frames: List<TelemetryFrame>) {
    val tail = frames.takeLast(120)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val pressureColor = NgColors.Primary
    val flowColor = NgColors.Secondary
    val tempColor = Color(0xFF8E24AA)
    val viscosityColor = Color(0xFF546E7A)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Telemetry", style = MaterialTheme.typography.titleSmall)
            if (tail.isNotEmpty()) {
                Text("${tail.size} samples", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (tail.isEmpty()) {
            Text("No telemetry yet.", style = MaterialTheme.typography.bodySmall)
            return
        }

        val last = tail.last()
        val metrics = listOf(
            "Pressure" to "${"%.1f".format(last.pressure.kpa)} kPa",
            "Flow" to "${"%.2f".format(last.flowRate.microlitersPerSecond)} uL/s",
            "Temp" to "${"%.1f".format(last.temperature.celsius)} C",
            "Viscosity" to "${"%.2f".format(last.viscosity.pascalSecond)} Pa·s"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(NgSpacing.Small),
            modifier = Modifier.padding(top = NgSpacing.Small, bottom = NgSpacing.Small)
        ) {
            items(metrics) { (label, value) ->
                NgMetricTile(label = label, value = value, modifier = Modifier.width(160.dp))
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val pressure = tail.map { it.pressure.kpa }
            val flow = tail.map { it.flowRate.microlitersPerSecond }
            val temp = tail.map { it.temperature.celsius }
            val viscosity = tail.map { it.viscosity.pascalSecond }

            fun mapY(value: Double, min: Double, max: Double): Float {
                val normalized = if (max == min) 0.5 else (value - min) / (max - min)
                return size.height * (1f - normalized.toFloat())
            }

            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            val rows = 4
            for (i in 0..rows) {
                val y = size.height * i / rows
                drawLine(gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f, pathEffect = dash)
            }

            val stepX = if (tail.size <= 1) 0f else size.width / (tail.size - 1)
            fun drawSeries(values: List<Double>, color: Color, width: Float) {
                val min = values.minOrNull() ?: 0.0
                val max = values.maxOrNull() ?: 1.0
                for (i in 1 until values.size) {
                    val x1 = stepX * (i - 1)
                    val x2 = stepX * i
                    drawLine(
                        color = color,
                        start = Offset(x1, mapY(values[i - 1], min, max)),
                        end = Offset(x2, mapY(values[i], min, max)),
                        strokeWidth = width
                    )
                }
                val lastIndex = values.lastIndex
                drawCircle(color, radius = 4f, center = Offset(stepX * lastIndex, mapY(values[lastIndex], min, max)))
            }

            drawSeries(pressure, pressureColor, 4f)
            drawSeries(flow, flowColor, 3f)
            drawSeries(temp, tempColor, 3f)
            drawSeries(viscosity, viscosityColor, 3f)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = NgSpacing.Small),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendDot(color = pressureColor, label = "Pressure")
            LegendDot(color = flowColor, label = "Flow")
            LegendDot(color = tempColor, label = "Temp")
            LegendDot(color = viscosityColor, label = "Viscosity")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.height(12.dp).width(12.dp)) {
            drawCircle(color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

