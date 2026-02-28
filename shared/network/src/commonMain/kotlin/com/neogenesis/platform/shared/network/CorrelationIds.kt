package com.neogenesis.platform.shared.network

import kotlinx.datetime.Clock
import kotlin.random.Random

object CorrelationIds {
    fun newId(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val entropy = Random.nextInt(1000, 9999)
        return "corr-$now-$entropy"
    }
}
