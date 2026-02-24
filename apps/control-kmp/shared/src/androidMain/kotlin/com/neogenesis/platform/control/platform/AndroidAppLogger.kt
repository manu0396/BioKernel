package com.neogenesis.platform.control.platform

import android.util.Log
import com.neogenesis.platform.shared.network.AppLogger
import com.neogenesis.platform.shared.network.LogLevel

class AndroidAppLogger : AppLogger {
    override fun log(level: LogLevel, message: String, metadata: Map<String, String>) {
        val rendered = if (metadata.isEmpty()) {
            message
        } else {
            message + " | " + metadata.entries.joinToString { "${it.key}=${it.value}" }
        }
        when (level) {
            LogLevel.DEBUG -> Log.d("RegenOps", rendered)
            LogLevel.INFO -> Log.i("RegenOps", rendered)
            LogLevel.WARN -> Log.w("RegenOps", rendered)
            LogLevel.ERROR -> Log.e("RegenOps", rendered)
        }
    }
}
