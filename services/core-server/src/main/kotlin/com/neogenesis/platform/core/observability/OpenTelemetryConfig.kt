package com.neogenesis.platform.core.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor

object OpenTelemetryConfig {
    fun init(serviceName: String, endpoint: String?): OpenTelemetry {
        val resource = Resource.getDefault().merge(
            Resource.create(io.opentelemetry.api.common.Attributes.of(AttributeKey.stringKey("service.name"), serviceName))
        )
        val builder = SdkTracerProvider.builder().setResource(resource)
        if (!endpoint.isNullOrBlank()) {
            val exporter = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build()
            builder.addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
        }
        val tracerProvider = builder.build()
        return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
    }
}
