package com.neogenesis.domain.model

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

data class SessionMetadata(
    val id: Long = 0,
    val patientId: String = "",
    val lastSync: String = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date())
)


