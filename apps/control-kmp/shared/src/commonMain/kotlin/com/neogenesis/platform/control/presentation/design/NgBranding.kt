package com.neogenesis.platform.control.presentation.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * NeoGenesis Control System S.L.U.
 * Brand concept: "NG" (new genesis) + orbit/nodes (control system).
 *
 * Implemented in Compose (commonMain) so it works on Android + desktop without resource wiring.
 */
@Composable
fun NgBrandTitle(
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        NgBrandMark(modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "NeoGenesis",
            style = MaterialTheme.typography.titleMedium,
        )
        if (!compact) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Control System",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun NgBrandMark(modifier: Modifier = Modifier) {
    val accent = NgColors.Primary
    val fg = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val c = Offset(r, r)

        // Orbit ring
        drawCircle(
            color = accent,
            radius = r * 0.92f,
            style = Stroke(width = r * 0.12f, cap = StrokeCap.Round),
            center = c,
        )

        // Orbit nodes
        fun node(angle: Float) {
            val x = c.x + (r * 0.92f) * cos(angle)
            val y = c.y + (r * 0.92f) * sin(angle)
            drawCircle(color = accent, radius = r * 0.12f, center = Offset(x, y))
        }
        node(angle = (-1.57f)) // top
        node(angle = (0.0f))   // right
        node(angle = (2.2f))   // lower-left

        // "N" monogram
        val leftX = c.x - r * 0.35f
        val rightX = c.x + r * 0.35f
        val topY = c.y - r * 0.45f
        val botY = c.y + r * 0.45f

        val sw = r * 0.14f
        drawLine(color = fg, start = Offset(leftX, topY), end = Offset(leftX, botY), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = fg, start = Offset(rightX, topY), end = Offset(rightX, botY), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = fg, start = Offset(leftX, botY), end = Offset(rightX, topY), strokeWidth = sw, cap = StrokeCap.Round)

        // Core/seed dot
        drawCircle(color = accent, radius = r * 0.10f, center = Offset(c.x, c.y + r * 0.10f))
    }
}

