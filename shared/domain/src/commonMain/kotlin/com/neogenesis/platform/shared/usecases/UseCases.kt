package com.neogenesis.platform.shared.usecases

import com.neogenesis.platform.shared.domain.*
import com.neogenesis.platform.shared.errors.DomainError
import com.neogenesis.platform.shared.errors.DomainResult
import com.neogenesis.platform.shared.evidence.EvidenceChainBuilder
import com.neogenesis.platform.shared.evidence.EvidenceExporter
import com.neogenesis.platform.shared.repositories.*
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import kotlinx.datetime.Clock

class StartPrintJobUseCase(
    private val printJobRepository: PrintJobRepository
) {
    suspend fun execute(job: PrintJob, parameters: Map<String, String>): DomainResult<PrintJob> {
        if (job.status != PrintJobStatus.CREATED) {
            return DomainResult.Failure(DomainError.ValidationError("Job must start in CREATED state"))
        }
        return DomainResult.Success(printJobRepository.create(job, parameters))
    }
}

class RecordTelemetryUseCase(
    private val telemetryRepository: TelemetryRepository
) {
    suspend fun execute(jobId: PrintJobId, deviceId: DeviceId, frame: TelemetryFrame): DomainResult<Unit> {
        telemetryRepository.append(jobId, deviceId, frame)
        return DomainResult.Success(Unit)
    }
}

class GenerateEvidencePackageUseCase(
    private val evidenceRepository: EvidenceRepository
) {
    suspend fun execute(jobId: PrintJobId): DomainResult<EvidencePackage> {
        val logs = evidenceRepository.list(jobId)
        if (logs.isEmpty()) return DomainResult.Failure(DomainError.NotFound("No evidence found"))
        val export = EvidenceExporter.export(logs.map {
            com.neogenesis.platform.shared.evidence.EvidenceEvent(
                id = it.id,
                timestamp = it.timestamp,
                actorId = it.actorId.value,
                deviceId = it.deviceId.value,
                jobId = it.jobId.value,
                eventType = it.eventType,
                payloadHash = it.payloadHash,
                hash = it.hash,
                prevHash = it.prevHash
            )
        })
        return DomainResult.Success(
            EvidencePackage(
                jobId = jobId,
                manifestJson = "{\"count\":${export.eventCount}}",
                dataJson = export.json,
                hash = export.lastHash,
                createdAt = Clock.System.now()
            )
        )
    }
}

class ValidatePrintParametersUseCase {
    fun execute(parameters: Map<String, String>): DomainResult<Unit> {
        if (parameters.isEmpty()) return DomainResult.Failure(DomainError.ValidationError("Parameters required"))
        return DomainResult.Success(Unit)
    }
}

class AppendAuditEventUseCase(
    private val evidenceRepository: EvidenceRepository,
    private val chainBuilder: EvidenceChainBuilder
) {
    suspend fun execute(jobId: PrintJobId, actorId: UserId, deviceId: DeviceId, eventType: String, payload: String): DomainResult<AuditLog> {
        val event = chainBuilder.createEvent(Clock.System.now(), actorId.value, deviceId.value, jobId.value, eventType, payload)
        val log = AuditLog(
            id = event.id,
            jobId = PrintJobId(event.jobId),
            actorId = UserId(event.actorId),
            deviceId = DeviceId(event.deviceId),
            eventType = event.eventType,
            payloadHash = event.payloadHash,
            hash = event.hash,
            prevHash = event.prevHash,
            timestamp = event.timestamp
        )
        evidenceRepository.append(log)
        chainBuilder.append(event)
        return DomainResult.Success(log)
    }
}
