package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DevicePolicy
import com.neogenesis.platform.shared.network.ApiResult
import com.neogenesis.platform.shared.network.NetworkError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class HttpDevicePolicyApi(
    private val client: HttpClient
) : DevicePolicyApi {
    override suspend fun getPolicy(): ApiResult<DevicePolicy> = runCatching {
        val response: DevicePolicy = client.get("/api/v1/device-policy").body()
        ApiResult.Success(response)
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }

    override suspend fun registerDevice(deviceInfo: DeviceInfo): ApiResult<DevicePolicy> = runCatching {
        val response: DevicePolicy = client.post("/api/v1/device/register") { setBody(deviceInfo) }.body()
        ApiResult.Success(response)
    }.getOrElse { ApiResult.Failure(NetworkError.UnknownError(it.message ?: "http_error")) }
}
