package com.neogenesis.session.manager

import android.content.Context
import android.content.SharedPreferences
import app.cash.turbine.test
import com.neogenesis.data_core.persistence.CryptoManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionManagerImplTest {

    private val cryptoManager: CryptoManager = mockk()
    private val context: Context = mockk(relaxed = true)
    private val sharedPrefs: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    private lateinit var sessionManager: SessionManagerImpl

    @Before
    fun setup() {
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor

        sessionManager = SessionManagerImpl(context, cryptoManager)
    }

    @Test
    fun `saveSession should encrypt token and persist in encrypted storage`() = runTest {
        // GIVEN
        val rawToken = "BIO-MASTER-TOKEN-001"
        val userId = "USER-ALPHA"
        val encryptedData = byteArrayOf(1, 2, 3)
        val iv = byteArrayOf(4, 5, 6)

        every { cryptoManager.encrypt(rawToken) } returns Pair(encryptedData, iv)

        // WHEN
        sessionManager.saveSession(rawToken, userId)

        // THEN
        verify { cryptoManager.encrypt(rawToken) }
        verify { editor.putString("auth_token", any()) }
        verify { editor.putString("auth_iv", any()) }
        verify { editor.commit() }
    }

    @Test
    fun `clear should wipe preferences and emit logout event`() = runTest {
        // GIVEN
        sessionManager.logoutEvents.test {
            // WHEN
            sessionManager.clear()

            // THEN
            assertEquals(Unit, awaitItem())
            verify { editor.clear() }
            assertNull(sessionManager.sessionMetadataFlow.value)
        }
    }

    @Test
    fun `initial session should be null if decryption fails`() = runTest {
        every { sharedPrefs.getString("auth_token", null) } returns "invalid_base64"
        every { cryptoManager.decrypt(any(), any()) } throws Exception("Decryption Error")
        val initialSession = sessionManager.sessionMetadataFlow.value
        assertNull(initialSession)
    }
}