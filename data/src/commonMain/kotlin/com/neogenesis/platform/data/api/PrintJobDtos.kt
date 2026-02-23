package com.neogenesis.platform.data.api

import com.neogenesis.platform.shared.domain.PrintJobStatus
import kotlinx.serialization.Serializable

@Serializable
data class CreatePrintJobRequestDto(
    val deviceId: String,
    val operatorId: String,
    val bioinkBatchId: String,
    val parameters: Map<String, String>
)

@Serializable
data class UpdatePrintJobStatusRequestDto(
    val status: PrintJobStatus
)
