package com.neogenesis.platform.core

import com.neogenesis.platform.core.grpc.RegenOpsProtocolService
import com.neogenesis.grpc.ListProtocolsRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ProtocolServiceTest {
    @Test
    fun listProtocolsReturnsSeed() = runBlocking {
        val service = RegenOpsProtocolService()
        val response = service.listProtocols(ListProtocolsRequest.newBuilder().build())
        assertTrue(response.protocolsCount >= 1)
    }
}
