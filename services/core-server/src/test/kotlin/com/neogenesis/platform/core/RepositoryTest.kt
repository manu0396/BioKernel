package com.neogenesis.platform.core

import com.neogenesis.platform.shared.domain.User
import com.neogenesis.platform.shared.domain.UserId
import com.neogenesis.platform.shared.domain.Role
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryTest {
    @Test
    fun userModelKeepsRoles() {
        val user = User(UserId("u1"), "operator", setOf(Role.OPERATOR), true, Clock.System.now())
        assertEquals(setOf(Role.OPERATOR), user.roles)
    }
}

