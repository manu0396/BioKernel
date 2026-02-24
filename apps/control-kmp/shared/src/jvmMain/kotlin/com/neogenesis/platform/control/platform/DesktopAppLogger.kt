package com.neogenesis.platform.control.platform

import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.LogLevel

class DesktopAppLogger : AppLogger {
    override fun log(level: LogLevel, message: String, metadata: Map<String, String>) {
        val rendered = if (metadata.isEmpty()) message else message + " | " + metadata.entries.joinToString { "${it.key}=${it.value}" }
        println("[${level.name}] $rendered")
    }
}
