package com.neogenesis.data_core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neogenesis.data_core.network.BioApiService
import com.neogenesis.data_core.util.AppLogger
import com.neurogenesis.shared_network.models.RetinaSampleDto
import kotlinx.io.IOException

class HazardWorker(
    context: Context,
    params: WorkerParameters,
    private val apiService: BioApiService
) : CoroutineWorker(context, params) {
    val TAG = "HazardWorker"
    override suspend fun doWork(): Result {
        return try {
            val patientId = "ESP-NODO-MAD"
            val response = apiService.fetchRetinaSamples(patientId)
            val lethalSample = response.find { it.toxicityScore >= 0.9 }

            if (lethalSample != null) {
                AppLogger.d(TAG, "¡ALERTA! Muestra letal detectada: ${lethalSample.id}")
                showHazardNotification(lethalSample)
            }
            Result.success()
        } catch (e: IOException) {
            Log.e(TAG, e.message,e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, e.message,e)
            Result.failure()
        }
    }

    private fun showHazardNotification(sample: RetinaSampleDto) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bio_hazard_alerts"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Bio-Hazard Alerts", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("ALERTA BIOLÓGICA: NIVEL LETAL")
            .setContentText("Detectada toxicidad crítica en el nodo ${sample.id}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(sample.id.hashCode(), notification)
    }
}