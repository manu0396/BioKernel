package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.RegenOpsRunService
import com.neogenesis.platform.proto.v1.GetRunRequest
import com.neogenesis.platform.proto.v1.StartRunRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RunServiceTest {
    @Test
    fun startRunCreatesRun() = runBlocking {
        val service = RegenOpsRunService()
        val run = service.startRun(
            StartRunRequest.newBuilder().setProtocolId("proto-1").setVersionId("v1").build()
        )
        val fetched = service.getRun(GetRunRequest.newBuilder().setRunId(run.runId).build())
        assertEquals(run.runId, fetched.runId)
    }
}
