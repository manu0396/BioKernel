package com.neogenesis.platform.control.device

import com.neogenesis.platform.shared.domain.device.DeviceInfo

expect fun detectDeviceInfo(appVersion: String, policyVersion: Int?): DeviceInfo
