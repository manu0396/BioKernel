package com.neogenesis.platform.control.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.neogenesis.platform.control.data.db.RegenOpsDatabase
import com.neogenesis.platform.shared.domain.Protocol
import com.neogenesis.platform.shared.domain.ProtocolId
import com.neogenesis.platform.shared.domain.ProtocolVersion
import com.neogenesis.platform.shared.domain.ProtocolVersionId
import com.neogenesis.platform.shared.domain.Run
import com.neogenesis.platform.shared.domain.RunId
import com.neogenesis.platform.shared.domain.RunStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

class RegenOpsLocalDataSource(
    private val database: RegenOpsDatabase
) {
    private val queries = database.regenOpsDatabaseQueries

    fun observeProtocols(): Flow<List<Protocol>> {
        return queries.selectProtocols(
            mapper = { id, name, summary, latestVersionId, updatedAt ->
                LocalProtocolRow(id, name, summary, latestVersionId, updatedAt)
            }
        )
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    val versions = queries.selectProtocolVersionsByProtocolId(
                        protocolId = row.id,
                        mapper = { vid, pid, version, createdAt, author, payload, published ->
                            LocalProtocolVersionRow(vid, pid, version, createdAt, author, payload, published)
                        }
                    ).executeAsList()
                    row.toDomain(versions)
                }
            }
    }

    fun observeRuns(limit: Long): Flow<List<Run>> {
        return queries.selectRuns(
            value = limit,
            mapper = { id, protocolId, protocolVersionId, status, createdAt, updatedAt ->
                LocalRunRow(id, protocolId, protocolVersionId, status, createdAt, updatedAt)
            }
        )
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
    }

    fun replaceProtocols(protocols: List<Protocol>, versions: List<ProtocolVersion>) {
        queries.transaction {
            queries.clearProtocols()
            queries.clearProtocolVersions()
            protocols.forEach { protocol ->
                queries.insertProtocol(
                    id = protocol.id.value,
                    name = protocol.name,
                    summary = protocol.summary,
                    latestVersionId = protocol.latestVersion?.id?.value,
                    updatedAt = (protocol.latestVersion?.createdAt ?: Instant.fromEpochMilliseconds(0)).toEpochMilliseconds()
                )
            }
            versions.forEach { version ->
                queries.insertProtocolVersion(
                    id = version.id.value,
                    protocolId = version.protocolId.value,
                    version = version.version,
                    createdAt = version.createdAt.toEpochMilliseconds(),
                    author = version.author,
                    payload = version.payload,
                    published = version.published
                )
            }
        }
    }

    fun replaceRuns(runs: List<Run>) {
        queries.transaction {
            queries.clearRuns()
            runs.forEach { run ->
                queries.insertRun(
                    id = run.id.value,
                    protocolId = run.protocolId.value,
                    protocolVersionId = run.protocolVersionId.value,
                    status = run.status.name,
                    createdAt = run.createdAt.toEpochMilliseconds(),
                    updatedAt = run.updatedAt?.toEpochMilliseconds()
                )
            }
        }
    }

    fun insertRun(run: Run) {
        queries.insertRun(
            id = run.id.value,
            protocolId = run.protocolId.value,
            protocolVersionId = run.protocolVersionId.value,
            status = run.status.name,
            createdAt = run.createdAt.toEpochMilliseconds(),
            updatedAt = run.updatedAt?.toEpochMilliseconds()
        )
    }

    fun insertProtocolVersion(version: ProtocolVersion) {
        queries.insertProtocolVersion(
            id = version.id.value,
            protocolId = version.protocolId.value,
            version = version.version,
            createdAt = version.createdAt.toEpochMilliseconds(),
            author = version.author,
            payload = version.payload,
            published = version.published
        )
    }

    fun insertProtocol(protocol: Protocol) {
        queries.insertProtocol(
            id = protocol.id.value,
            name = protocol.name,
            summary = protocol.summary,
            latestVersionId = protocol.latestVersion?.id?.value,
            updatedAt = (protocol.latestVersion?.createdAt ?: Instant.fromEpochMilliseconds(0)).toEpochMilliseconds()
        )
    }
}

private data class LocalProtocolRow(
    val id: String,
    val name: String,
    val summary: String,
    val latestVersionId: String?,
    val updatedAt: Long
) {
    fun toDomain(versions: List<LocalProtocolVersionRow>): Protocol {
        val versionModels = versions.map { it.toDomain() }
        val latest = versionModels.firstOrNull { it.id.value == latestVersionId } ?: versionModels.firstOrNull()
        return Protocol(
            id = ProtocolId(id),
            name = name,
            summary = summary,
            latestVersion = latest,
            versions = versionModels
        )
    }
}

private data class LocalProtocolVersionRow(
    val id: String,
    val protocolId: String,
    val version: String,
    val createdAt: Long,
    val author: String,
    val payload: String,
    val published: Boolean
) {
    fun toDomain(): ProtocolVersion {
        return ProtocolVersion(
            id = ProtocolVersionId(id),
            protocolId = ProtocolId(protocolId),
            version = version,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            author = author,
            payload = payload,
            published = published
        )
    }
}

private data class LocalRunRow(
    val id: String,
    val protocolId: String,
    val protocolVersionId: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long?
) {
    fun toDomain(): Run {
        val parsedStatus = runCatching { RunStatus.valueOf(status) }.getOrElse { RunStatus.FAILED }
        return Run(
            id = RunId(id),
            protocolId = ProtocolId(protocolId),
            protocolVersionId = ProtocolVersionId(protocolVersionId),
            status = parsedStatus,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = updatedAt?.let { Instant.fromEpochMilliseconds(it) }
        )
    }
}
