package com.neogenesis.platform.core

import com.neogenesis.grpc.GatewayTelemetry
import com.neogenesis.platform.core.grpc.RegenOpsInMemoryStore
import com.neogenesis.platform.core.observability.MetricLabels
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class MockGatewayTelemetryFlowTest {
    @Test
    fun mockGatewayTelemetryIsStreamedOnlyForTheRequestedRunId() = runBlocking {
        val runId = "mock-run-${UUID.randomUUID()}"
        val otherRunId = "mock-run-${UUID.randomUUID()}"
        val run = RegenOpsInMemoryStore.startRun(
            protocolId = "proto-1",
            version = 1,
            requestedRunId = runId,
            gatewayId = "mock-gateway",
            labels = MetricLabels()
        )
        val received = async(start = CoroutineStart.UNDISPATCHED) {
            RegenOpsInMemoryStore.telemetry(runId).first()
        }
        RegenOpsInMemoryStore.pushGatewayTelemetry(
            "mock-gateway",
            listOf(
                GatewayTelemetry.newBuilder()
                    .setRunId(otherRunId)
                    .setMetricKey("pressure_kpa")
                    .setMetricValue(110.0)
                    .setUnit("kPa")
                    .setRecordedAtMs(1_800_000_000_000)
                    .setSeq(1)
                    .build(),
                GatewayTelemetry.newBuilder()
                    .setRunId(runId)
                    .setMetricKey("pressure_kpa")
                    .setMetricValue(111.0)
                    .setUnit("kPa")
                    .setRecordedAtMs(1_800_000_000_001)
                    .setSeq(2)
                    .build()
            )
        )

        val frame = withTimeout(1_000) { received.await() }

        assertEquals(runId, run.runId)
        assertEquals(runId, frame.runId)
        assertEquals("mock-gateway", frame.gatewayId)
        assertEquals(111.0, frame.metricValue)
        assertEquals(2, frame.seq)
    }
}
