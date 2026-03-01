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
import com.neogenesis.grpc.ListProtocolsRequest
import com.neogenesis.grpc.ProtocolServiceGrpcKt
import com.neogenesis.grpc.ProtocolSummary
import com.neogenesis.grpc.ProtocolVersionRecord
import com.neogenesis.grpc.PublishVersionRequest
import com.neogenesis.grpc.RunControlRequest
import com.neogenesis.grpc.RunRecord
import com.neogenesis.grpc.RunServiceGrpcKt
import com.neogenesis.grpc.StartRunRequest
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.datetime.Clock

class GrpcControlApi(
    config: AppConfig,
    deviceInfoStore: DeviceInfoStore
) : ControlApi {
    private val channel: ManagedChannel = OkHttpChannelBuilder.forAddress(config.grpcHost, config.grpcPort)
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
            PublishVersionRequest.newBuilder()
                .setProtocolId(protocolId)
                .setChangelog("publish $versionId")
                .build()
        )
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    override suspend fun startRun(protocolId: String, versionId: String): ApiResult<Run> = runCatching {
        val protocolVersion = versionId.filter(Char::isDigit).toIntOrNull() ?: 1
        val response = runStub.startRun(
            StartRunRequest.newBuilder()
                .setProtocolId(protocolId)
                .setProtocolVersion(protocolVersion)
                .build()
        )
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    override suspend fun pauseRun(runId: String): ApiResult<Run> = runCatching {
        val response = runStub.pauseRun(RunControlRequest.newBuilder().setRunId(runId).build())
        ApiResult.Success(response.toDomain())
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "grpc_error")) }

    override suspend fun abortRun(runId: String): ApiResult<Run> = runCatching {
        val response = runStub.abortRun(RunControlRequest.newBuilder().setRunId(runId).setReason("user_request").build())
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

private fun ProtocolSummary.toDomain(): Protocol {
    val latest = ProtocolVersion(
        id = ProtocolVersionId("${protocolId}-v$latestVersion"),
        protocolId = ProtocolId(protocolId),
        version = latestVersion.toString(),
        createdAt = Clock.System.now(),
        author = "system",
        payload = "",
        published = true
    )
    return Protocol(
        id = ProtocolId(protocolId),
        name = title,
        summary = "",
        latestVersion = latest,
        versions = listOf(latest)
    )
}

private fun ProtocolVersionRecord.toDomain(): ProtocolVersion {
    return ProtocolVersion(
        id = ProtocolVersionId("${protocolId}-v$version"),
        protocolId = ProtocolId(protocolId),
        version = version.toString(),
        createdAt = Clock.System.now(),
        author = publishedBy.ifBlank { "system" },
        payload = contentJson,
        published = true
    )
}

private fun RunRecord.toDomain(): Run {
    val status = runCatching { RunStatus.valueOf(status) }.getOrElse { RunStatus.PENDING }
    return Run(
        id = RunId(runId),
        protocolId = ProtocolId(protocolId),
        protocolVersionId = ProtocolVersionId(protocolVersion.toString()),
        status = status,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
}
