package com.neogenesis.platform.control.presentation.report

import com.neogenesis.platform.control.presentation.SimulationConfig
import com.neogenesis.platform.shared.domain.RunEvent
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.datetime.Clock

/**
 * Minimal dependency-free single-page PDF generator.
 */
object SimulationReportPdf {
    fun build(
        runId: String,
        config: SimulationConfig?,
        events: List<RunEvent>,
        telemetry: List<TelemetryFrame>,
    ): ByteArray {
        val lines = mutableListOf<String>()
        lines += "NeoGenesis Control System S.L.U."
        lines += "Simulation Report"
        lines += "Run ID: $runId"
        lines += "Generated: ${Clock.System.now()}"
        lines += ""

        config?.let {
            lines += "Simulation configuration"
            lines += "  Duration (min): ${it.durationMinutes}"
            lines += "  Tick (ms): ${it.tickMillis}"
            lines += "  Speed factor: ${it.speedFactor}"
            if (it.operatorName.isNotBlank()) lines += "  Operator: ${it.operatorName}"
            if (it.notes.isNotBlank()) lines += "  Notes: ${it.notes}"
            lines += ""
        }

        lines += "Events (${events.size})"
        events.asReversed().take(60).forEach { e ->
            val msg = e.message.replace("\n", " ").take(120)
            lines += "  ${e.createdAt} | ${e.eventType} | $msg"
        }
        lines += ""

        lines += "Telemetry"
        lines += "  Frames: ${telemetry.size}"
        telemetry.takeLast(5).forEachIndexed { idx, frame ->
            lines += "  [${telemetry.size - 5 + idx + 1}] ${frame.toString().replace("\n", " ").take(140)}"
        }

        return SimplePdf.fromLines(lines)
    }
}

private object SimplePdf {
    fun fromLines(lines: List<String>): ByteArray {
        val safeLines = lines.take(85)
        val content = buildString {
            append("BT\n/F1 11 Tf\n72 800 Td\n14 TL\n")
            for (line in safeLines) {
                append("(").append(escape(line)).append(") Tj\nT*\n")
            }
            append("ET\n")
        }

        val objects = mutableListOf<String>()
        fun add(obj: String) { objects += obj }
        add("<< /Type /Catalog /Pages 2 0 R >>")
        add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>")
        add("<< /Length ${content.encodeToByteArray().size} >>\nstream\n$content\nendstream")
        add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

        val out = StringBuilder()
        out.append("%PDF-1.4\n")
        val offsets = IntArray(objects.size + 1)
        for ((i, obj) in objects.withIndex()) {
            offsets[i + 1] = out.length
            out.append("${i + 1} 0 obj\n").append(obj).append("\nendobj\n")
        }
        val xrefStart = out.length
        out.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        for (i in 1..objects.size) {
            out.append(offsets[i].toString().padStart(10, '0')).append(" 00000 n \n")
        }
        out.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n")
        out.append(xrefStart).append("\n%%EOF\n")
        return out.toString().encodeToByteArray()
    }

    private fun escape(s: String): String {
        val ascii = s.map { ch -> if (ch.code in 32..126) ch else '?' }.joinToString("")
        return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
    }
}

