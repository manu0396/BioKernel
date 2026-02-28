package com.neogenesis.platform.shared.usecases

import com.neogenesis.platform.shared.domain.BioinkBatchId
import com.neogenesis.platform.shared.domain.DeviceId
import com.neogenesis.platform.shared.domain.PrintJob
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.domain.PrintJobStatus
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.shared.errors.DomainResult
import com.neogenesis.platform.shared.repositories.PrintJobRepository
import kotlinx.datetime.Clock
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class UseCasesTest {
    @Test
    fun startPrintJobRejectsNonCreatedStatus() = runBlocking {
        val repo = FakePrintJobRepository()
        val useCase = StartPrintJobUseCase(repo)
        val job = PrintJob(
            id = PrintJobId("job-1"),
            deviceId = DeviceId("dev-1"),
            operatorId = UserId("user-1"),
            bioinkBatchId = BioinkBatchId("batch-1"),
            createdAt = Clock.System.now(),
            status = PrintJobStatus.RUNNING
        )
        val result = useCase.execute(job, emptyMap())
        assertTrue(result is DomainResult.Failure)
    }

    @Test
    fun validatePrintParametersRejectsEmptyMap() {
        val useCase = ValidatePrintParametersUseCase()
        val result = useCase.execute(emptyMap())
        assertTrue(result is DomainResult.Failure)
    }

    private class FakePrintJobRepository : PrintJobRepository {
        override suspend fun create(job: PrintJob, parameters: Map<String, String>): PrintJob = job
        override suspend fun updateStatus(id: PrintJobId, status: PrintJobStatus) = Unit
        override suspend fun findById(id: PrintJobId): PrintJob? = null
        override suspend fun list(limit: Int): List<PrintJob> = emptyList()
    }
}
