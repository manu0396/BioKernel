package com.neogenesis.data_core.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.neogenesis.data_core.network.BioApiService
import com.neurogenesis.shared_network.models.RetinaSampleDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HazardWorkerTest {
    private lateinit var context: Context
    private val apiService: BioApiService = mockk()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `when lethal toxicity is detected worker should return success`() = runBlocking {
        // GIVEN: Una muestra con toxicidad del 95% (LETAL)
        val mockSamples = listOf(
            RetinaSampleDto(id = "TEST-ID", patientId = "P1", toxicityScore = 0.95, timestamp = "2024-01-01T00:00:00Z")
        )
        coEvery { apiService.fetchRetinaSamples(any()) } returns mockSamples

        val worker = TestListenableWorkerBuilder<HazardWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
                    return HazardWorker(appContext, workerParameters, apiService)
                }
            })
            .build()

        // WHEN
        val result = worker.doWork()

        // THEN
        assertEquals(ListenableWorker.Result.success(), result)
    }
}