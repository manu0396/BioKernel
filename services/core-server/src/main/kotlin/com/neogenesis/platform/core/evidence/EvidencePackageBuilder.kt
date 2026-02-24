package com.neogenesis.platform.core.evidence

import com.neogenesis.platform.core.config.AppConfig
import com.neogenesis.platform.core.config.VersionInfo
import com.neogenesis.platform.core.storage.EvidenceRepositoryImpl
import com.neogenesis.platform.core.storage.TelemetryRepositoryImpl
import com.neogenesis.platform.shared.domain.PrintJobId
import com.neogenesis.platform.shared.evidence.EvidenceChainValidator
import com.neogenesis.platform.shared.evidence.EvidenceEvent
import com.neogenesis.platform.shared.telemetry.TelemetryAggregator
import com.neogenesis.platform.shared.telemetry.TelemetryDownsampler
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EvidencePackageBuilder(
    private val config: AppConfig,
    private val evidenceRepository: EvidenceRepositoryImpl,
    private val telemetryRepository: TelemetryRepositoryImpl
) {
    private val json = Json { encodeDefaults = true }

    suspend fun build(jobId: PrintJobId): EvidenceZipBundle {
        val logs = evidenceRepository.list(jobId).sortedBy { it.timestamp }
        val events = logs.map {
            EvidenceEvent(
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
        }
        val telemetry = telemetryRepository.list(jobId, 10_000)
        val downsampled = TelemetryDownsampler(windowMs = 1_000).downsample(telemetry)
        val summary = if (telemetry.isNotEmpty()) TelemetryAggregator().aggregate(telemetry) else null

        val metadata = EvidenceMetadata(
            jobId = jobId.value,
            createdAtMs = Clock.System.now().toEpochMilliseconds(),
            appVersion = VersionInfo.version,
            environment = config.environment,
            configSnapshot = mapOf(
                "HTTP_PORT" to config.httpPort.toString(),
                "GRPC_PORT" to config.grpcPort.toString(),
                "GRPC_ENABLED" to config.grpcEnabled.toString(),
                "DB_URL" to config.database.url,
                "JWT_ISSUER" to config.jwt.issuer,
                "JWT_AUDIENCE" to config.jwt.audience
            )
        )

        val summaryJson = summary?.let { json.encodeToString(it) } ?: "null"
        val files = linkedMapOf(
            "metadata.json" to json.encodeToString(metadata).toByteArray(),
            "audit.json" to json.encodeToString(events).toByteArray(),
            "telemetry_samples.json" to json.encodeToString(downsampled).toByteArray(),
            "telemetry_summary.json" to summaryJson.toByteArray()
        )

        val manifest = EvidenceManifest(
            jobId = jobId.value,
            createdAtMs = metadata.createdAtMs,
            auditChainValid = EvidenceChainValidator.validate(events),
            lastHash = events.lastOrNull()?.hash,
            hashAlgorithm = "SHA-256",
            files = files.map { (name, content) ->
                ManifestEntry(path = name, sha256 = sha256Hex(content), size = content.size.toLong())
            }
        )
        files["manifest.json"] = json.encodeToString(manifest).toByteArray()

        return EvidenceZipBundle(
            fileName = "evidence-${jobId.value}.zip",
            bytes = zip(files)
        )
    }

    private fun sha256Hex(content: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun zip(files: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.toSortedMap().forEach { (name, content) ->
                val entry = ZipEntry(name).apply { time = 0L }
                zip.putNextEntry(entry)
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}

@Serializable
data class EvidenceMetadata(
    val jobId: String,
    val createdAtMs: Long,
    val appVersion: String,
    val environment: String,
    val configSnapshot: Map<String, String>
)

@Serializable
data class EvidenceManifest(
    val jobId: String,
    val createdAtMs: Long,
    val auditChainValid: Boolean,
    val lastHash: String?,
    val hashAlgorithm: String,
    val files: List<ManifestEntry>
)

@Serializable
data class ManifestEntry(
    val path: String,
    val sha256: String,
    val size: Long
)

data class EvidenceZipBundle(
    val fileName: String,
    val bytes: ByteArray
)

