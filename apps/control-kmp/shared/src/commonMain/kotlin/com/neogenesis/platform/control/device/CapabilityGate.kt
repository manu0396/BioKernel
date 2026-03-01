package com.neogenesis.platform.control.device

import com.neogenesis.platform.control.presentation.AppScreen
import com.neogenesis.platform.shared.domain.device.Capability

class CapabilityGate(
    private val capabilities: Set<Capability>
) {
    fun can(required: Capability): Boolean = capabilities.contains(required)

    fun canAccess(screen: AppScreen): Boolean {
        val required = when (screen) {
            AppScreen.PROTOCOLS -> Capability.READ_ONLY_DASHBOARD
            AppScreen.PROTOCOL_DETAIL -> Capability.READ_ONLY_DASHBOARD
            AppScreen.RUN_CONTROL -> Capability.PRINT_CONTROL
            AppScreen.LIVE_RUN -> Capability.LIVE_MONITOR
            AppScreen.EXPORTS -> Capability.QC_REVIEW
            AppScreen.TRACE -> Capability.QC_REVIEW
            AppScreen.COMMERCIAL -> Capability.READ_ONLY_DASHBOARD
            AppScreen.AUTH -> null
            AppScreen.UNSUPPORTED -> null
        }
        return required == null || capabilities.contains(required)
    }
}
