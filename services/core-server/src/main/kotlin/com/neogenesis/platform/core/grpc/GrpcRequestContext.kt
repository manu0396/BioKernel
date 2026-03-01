package com.neogenesis.platform.core.grpc

import com.neogenesis.platform.core.observability.MetricLabels
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerInterceptor

object GrpcRequestContext {
    private val tenantKey = Context.key<String>("tenant_id")
    private val siteKey = Context.key<String>("site_id")
    private val cohortKey = Context.key<String>("cohort_id")
    private val protocolKey = Context.key<String>("protocol_id")
    private val protocolVersionKey = Context.key<String>("protocol_version")

    private val tenantHeader = Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER)
    private val siteHeader = Metadata.Key.of("x-site-id", Metadata.ASCII_STRING_MARSHALLER)
    private val cohortHeader = Metadata.Key.of("x-cohort-id", Metadata.ASCII_STRING_MARSHALLER)
    private val protocolHeader = Metadata.Key.of("x-protocol-id", Metadata.ASCII_STRING_MARSHALLER)
    private val protocolVersionHeader = Metadata.Key.of("x-protocol-version", Metadata.ASCII_STRING_MARSHALLER)

    fun interceptor(): ServerInterceptor = object : ServerInterceptor {
        override fun <ReqT : Any?, RespT : Any?> interceptCall(
            call: io.grpc.ServerCall<ReqT, RespT>,
            headers: Metadata,
            next: io.grpc.ServerCallHandler<ReqT, RespT>
        ): io.grpc.ServerCall.Listener<ReqT> {
            val ctx = Context.current()
                .withValue(tenantKey, headers.get(tenantHeader) ?: "unknown")
                .withValue(siteKey, headers.get(siteHeader) ?: "unknown")
                .withValue(cohortKey, headers.get(cohortHeader) ?: "unknown")
                .withValue(protocolKey, headers.get(protocolHeader) ?: "unknown")
                .withValue(protocolVersionKey, headers.get(protocolVersionHeader) ?: "unknown")
            return Contexts.interceptCall(ctx, call, headers, next)
        }
    }

    fun currentLabels(protocolId: String? = null, protocolVersion: Int? = null): MetricLabels {
        val base = MetricLabels(
            tenantId = tenantKey.get() ?: "unknown",
            siteId = siteKey.get() ?: "unknown",
            cohortId = cohortKey.get() ?: "unknown",
            protocolId = protocolKey.get() ?: "unknown",
            protocolVersion = protocolVersionKey.get() ?: "unknown"
        )
        return base.withProtocol(protocolId, protocolVersion)
    }
}
