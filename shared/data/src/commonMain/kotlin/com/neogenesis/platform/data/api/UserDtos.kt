package com.neogenesis.platform.data.api

import com.neogenesis.platform.shared.domain.Role
import com.neogenesis.platform.shared.domain.User
import com.neogenesis.platform.shared.domain.UserId
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val roles: Set<Role>,
    val active: Boolean,
    val createdAtEpochMs: Long
)

fun UserDto.toDomain(): User = User(
    id = UserId(id),
    username = username,
    roles = roles,
    active = active,
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMs)
)
