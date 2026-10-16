package com.neogenesis.platform.gateway

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeviceGatewayTest {
    @Test
    fun mockTickKeepsTheRequestedRunIdAndSequenceAcrossEventAndTelemetry() {
        val tick = createGatewayTick("mock-run-17", tick = 17, recordedAtMs = 1_800_000_000_000)

        assertEquals("mock-run-17", tick.event.runId)
        assertEquals(17L, tick.event.seq)
        assertEquals(2, tick.telemetry.size)
        assertEquals(setOf("mock-run-17"), tick.telemetry.map { it.runId }.toSet())
        assertEquals(setOf(17L), tick.telemetry.map { it.seq }.toSet())
        assertEquals(setOf(1_800_000_000_000), tick.telemetry.map { it.recordedAtMs }.toSet())
    }

    @Test
    fun mockTickRejectsMissingRunIds() {
        assertFailsWith<IllegalArgumentException> {
            createGatewayTick(" ", tick = 0, recordedAtMs = 1)
        }
    }
}
