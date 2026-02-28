package com.neogenesis.platform.core.config

import java.nio.file.Files
import java.nio.file.Paths

object VersionInfo {
    val version: String = run {
        val env = System.getenv("APP_VERSION") ?: System.getProperty("APP_VERSION")
        if (!env.isNullOrBlank()) return@run env
        val path = Paths.get("VERSION")
        if (Files.exists(path)) {
            Files.readString(path).trim().ifBlank { "0.0.0" }
        } else {
            "0.0.0"
        }
    }
}

