package com.neogenesis.platform.control.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object FileNameUtils {
    private val invalidCharsRegex = Regex("""[\\/:*?"<>|]""")

    fun sanitizeFileName(baseName: String): String {
        val sanitizedBase =
            baseName
                .trim()
                .replace(invalidCharsRegex, "_")
                .ifBlank { "file" }

        return "${sanitizedBase}_${currentTimestamp()}"
    }

    fun getTelemetryExportFileName(baseName: String = "telemetry"): String =
        sanitizeFileName(baseName) + ".xlsx"

    private fun currentTimestamp(): String {
        val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        fun two(n: Int) = n.toString().padStart(2, '0')

        return buildString {
            append(dt.year)
            append("-")
            append(two(dt.monthNumber))
            append("-")
            append(two(dt.dayOfMonth))
            append("_")
            append(two(dt.hour))
            append("-")
            append(two(dt.minute))
            append("-")
            append(two(dt.second))
        }
    }
}
