package com.neogenesis.platform.control.data.remote

import com.neogenesis.platform.shared.domain.device.DeviceInfo
import com.neogenesis.platform.shared.domain.device.DevicePolicy
import com.neogenesis.platform.shared.network.ApiResult

interface DevicePolicyApi {
    suspend fun getPolicy(): ApiResult<DevicePolicy>
    suspend fun registerDevice(deviceInfo: DeviceInfo): ApiResult<DevicePolicy>
}
