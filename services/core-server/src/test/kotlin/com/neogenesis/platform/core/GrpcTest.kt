package com.neogenesis.platform.core

import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class GrpcTest {
    @Test
    fun inProcessServerStarts() {
        val name = InProcessServerBuilder.generateName()
        val server = InProcessServerBuilder.forName(name).directExecutor().build().start()
        val channel = InProcessChannelBuilder.forName(name).directExecutor().build()
        assertNotNull(server)
        channel.shutdownNow()
        server.shutdownNow()
    }
}

