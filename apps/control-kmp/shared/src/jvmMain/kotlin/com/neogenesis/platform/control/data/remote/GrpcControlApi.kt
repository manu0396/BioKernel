package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.control.AppConfig
import com.neogenesis.platform.control.device.DeviceInfoStore
import com.neogenesis.platform.control.device.GrpcDeviceHeaders
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import com.neogenesis.platform.proto.v1.GetRunRequest
import com.neogenesis.platform.proto.v1.ListProtocolsRequest
import com.neogenesis.platform.proto.v1.ProtocolServiceGrpcKt
import com.neogenesis.platform.proto.v1.RunServiceGrpcKt
import com.neogenesis.platform.proto.v1.StartRunRequest
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class GrpcControlApi(
    config: AppConfig,
    deviceInfoStore: DeviceInfoStore
) : ControlApi {
    private val channel: ManagedChannel = NettyChannelBuilder.forAddress(config.grpcHost, config.grpcPort)
        .apply { if (!config.grpcUseTls) usePlaintext() }
        .build()

    private val protocolStub = ProtocolServiceGrpcKt.ProtocolServiceCoroutineStub(channel)
        .withInterceptors(DeviceMetadataInterceptor(deviceInfoStore))
    private val runStub = RunServiceGrpcKt.RunServiceCoroutineStub(channel)
        .withInterceptors(DeviceMetadataInterceptor(deviceInfoStore))

    override suspend fun listProtocols(): ApiResult<List<Protocol>> = runCatching {
        val response = protocolStub.listProtocols(ListProtocolsRequest.newBuilder().build())
        ApiResult.Success(response.protocolsList.map { it.toDomain() })
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    
    override suspend fun createProtocol(request: CreateProtocolRequest): ApiResult<Protocol> =
        ApiResult.Failure(NetworkError.UnknownError("grpc_not_supported"))

    
    override suspend fun updateProtocolStatus(protocolId: String, status: String): ApiResult<Protocol> =
        ApiResult.Failure(NetworkError.UnknownError("grpc_not_supported"))

    override suspend fun listRuns(): ApiResult<List<Run>> = ApiResult.Success(emptyList())

    override suspend fun publishVersion(protocolId: String, versionId: String): ApiResult<ProtocolVersion> = runCatching {
        val response = protocolStub.publishVersion(
            com.neogenesis.platform.proto.v1.PublishVersionRequest.newBuilder()
                .setProtocolId(protocolId)
                .setVersionId(versionId)
                .build()
        )
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    override suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run> = runCatching {
        val response = runStub.startRun(
            StartRunRequest.newBuilder().setProtocolId(protocolId).setVersionId(versionId).build()
        )
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    override suspend fun pauseRun(runId: String): ApiResult<Run> = runCatching {
        val response = runStub.pauseRun(com.neogenesis.platform.proto.v1.PauseRunRequest.newBuilder().setRunId(runId).build())
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    override suspend fun abortRun(runId: String): ApiResult<Run> = runCatching {
        val response = runStub.abortRun(com.neogenesis.platform.proto.v1.AbortRunRequest.newBuilder().setRunId(runId).build())
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    fun close() = channel.shutdownNow()
}

private class DeviceMetadataInterceptor(
    private val deviceInfoStore: DeviceInfoStore
) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val call = next.newCall(method, callOptions)
        return object : ClientCall<ReqT, RespT>() {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                GrpcDeviceHeaders.apply(headers, deviceInfoStore.get())
                call.start(responseListener, headers)
            }

            override fun request(numMessages: Int) = call.request(numMessages)
            override fun cancel(message: String?, cause: Throwable?) = call.cancel(message, cause)
            override fun halfClose() = call.halfClose()
            override fun sendMessage(message: ReqT) = call.sendMessage(message)
        }
    }
}

private fun com.neogenesis.platform.proto.v1.ProtocolSummary.toDomain(): Protocol {
    val latest = latestVersion.toDomain()
    return Protocol(
        id = ProtocolId(protocolId),
        name = name,
        summary = summary,
        latestVersion = latest,
        versions = listOf(latest)
    )
}

private fun com.neogenesis.platform.proto.v1.ProtocolVersion.toDomain(): ProtocolVersion {
    return ProtocolVersion(
        id = ProtocolVersionId(versionId),
        protocolId = ProtocolId(protocolId),
        version = version,
        createdAt = runCatching { Instant.parse(createdAt) }.getOrElse { Clock.System.now() },
        author = "system",
        payload = payload,
        published = published
    )
}

private fun com.neogenesis.platform.proto.v1.RunRef.toDomain(): Run {
    val status = runCatching { RunStatus.valueOf(status) }.getOrElse { RunStatus.PENDING }
    return Run(
        id = RunId(runId),
        protocolId = ProtocolId(""),
        protocolVersionId = ProtocolVersionId(""),
        status = status,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
