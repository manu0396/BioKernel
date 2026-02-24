package com.neogenesis.platform.core.grpc

import java.util.concurrent.atomic.AtomicLong

class TelemetryWatchdog(private val timeoutMs: Long) {
    private val lastSeen = AtomicLong(0)

    fun mark(timestampMs: Long) {
        lastSeen.set(timestampMs)
    }

    fun isExpired(nowMs: Long): Boolean {
        val last = lastSeen.get()
        return last > 0 && (nowMs - last) > timeoutMs
    }
}

