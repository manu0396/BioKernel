package com.neogenesis.data_core.network

import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neogenesis.data_core.worker.HazardWorker
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.inject

class BioMessagingService : FirebaseMessagingService() {

    private val workManager: WorkManager by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        val sampleId = message.data["sampleId"] ?: "UNKNOWN"
        val toxicity = message.data["toxicity"]?.toDoubleOrNull() ?: 0.0

        val inputData = Data.Builder()
            .putString("KEY_SAMPLE_ID", sampleId)
            .putDouble("KEY_TOXICITY", toxicity)
            .build()

        val hazardRequest = OneTimeWorkRequestBuilder<HazardWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .build()

        workManager.enqueue(hazardRequest)
    }
}