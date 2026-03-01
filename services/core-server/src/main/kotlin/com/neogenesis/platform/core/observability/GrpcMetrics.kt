package com.neogenesis.platform.core.observability

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

class GrpcMetricsInterceptor(private val registry: MeterRegistry) : ServerInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val start = System.nanoTime()
        val fullMethod = call.methodDescriptor.fullMethodName
        val service = fullMethod.substringBefore('/')
        val method = fullMethod.substringAfter('/')
        val tagsBase = listOf("grpc_service", service, "grpc_method", method)
        val countingCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                val statusCode = status.code.name
                val tags = tagsBase + listOf("status", statusCode)
                registry.counter("neogenesis_grpc_requests_total", *tags.toTypedArray()).increment()
                Timer.builder("neogenesis_grpc_request_duration_ms")
                    .publishPercentileHistogram()
                    .tags(*tags.toTypedArray())
                    .register(registry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS)
                super.close(status, trailers)
            }
        }
        return next.startCall(countingCall, headers)
    }
}
