package com.neogenesis.platform.shared.network

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

interface AppLogger {
    fun log(level: LogLevel, message: String, metadata: Map<String, String> = emptyMap())
}

object NoOpLogger : AppLogger {
    override fun log(level: LogLevel, message: String, metadata: Map<String, String>) = Unit
}

object Redaction {
    fun value(raw: String, keepStart: Int = 3): String {
        if (raw.length <= keepStart) return "***"
        return raw.take(keepStart) + "***"
    }
}
