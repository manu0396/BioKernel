package com.neogenesis.platform.control.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object FileNameUtils {
    private const val INVALID_FILENAME_CHARS = "[/\:"*?<>|]"
    private const val TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss"

    fun sanitizeFileName(baseName: String): String {
        val sanitizedBase = baseName.replace(INVALID_FILENAME_CHARS.toRegex(), "_")
        val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(TIMESTAMP_FORMAT)
        return "${sanitizedBase}_$timestamp"
    }

    fun getTelemetryExportFileName(baseName: String = "telemetry"): String {
        return sanitizeFileName(baseName) + ".xlsx"
    }
}
