package com.neogenesis.platform.backend.security

import java.util.concurrent.ConcurrentHashMap

class RateLimiter(private val windowMs: Long, private val maxRequests: Int) {
    private data class Bucket(var count: Int, var windowStart: Long)
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun allow(key: String, now: Long = System.currentTimeMillis()): Boolean {
        val bucket = buckets.computeIfAbsent(key) { Bucket(0, now) }
        synchronized(bucket) {
            if (now - bucket.windowStart > windowMs) {
                bucket.windowStart = now
                bucket.count = 0
            }
            bucket.count += 1
            return bucket.count <= maxRequests
        }
    }
}
