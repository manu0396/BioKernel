package com.neogenesis.domain.mapper

import android.util.Log
import com.neogenesis.domain.model.RetinaAnalysis
import com.neogenesis.domain.model.RetinaRecord
import java.text.SimpleDateFormat
import java.util.Date

fun RetinaAnalysis.toRecord(): RetinaRecord {
    val formattedDate = try {
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", java.util.Locale.getDefault())
        val netDate = Date(this.timestamp)
        sdf.format(netDate)
    } catch (e: Exception) {
        Log.e("RetinaAnalysis.toRecord()", e.message ?: "Unknown Error")
        "Unknown Date"
    }

    return RetinaRecord(
        id = this.id,
        date = formattedDate,
        score = this.compatibilityScore.toInt(),
        toxicity = this.toxicity.name,
        diagnosisDetails = this.notes
    )
}


