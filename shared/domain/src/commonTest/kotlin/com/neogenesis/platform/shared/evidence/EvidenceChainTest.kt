package com.neogenesis.platform.shared.evidence

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvidenceChainTest {
    @Test
    fun hashChainValidates() {
        val builder = EvidenceChainBuilder()
        val e1 = builder.createEvent(Clock.System.now(), "actor", "device", "job", "START", "payload")
        builder.append(e1)
        val e2 = builder.createEvent(Clock.System.now(), "actor", "device", "job", "STEP", "payload2")
        builder.append(e2)
        assertTrue(EvidenceChainValidator.validate(builder.events()))
    }

    @Test
    fun tamperDetectionFailsValidation() {
        val builder = EvidenceChainBuilder()
        val e1 = builder.createEvent(Clock.System.now(), "actor", "device", "job", "START", "payload")
        builder.append(e1)
        val e2 = builder.createEvent(Clock.System.now(), "actor", "device", "job", "STEP", "payload2")
        val tampered = e2.copy(payloadHash = "deadbeef")
        assertFalse(EvidenceChainValidator.validate(listOf(e1, tampered)))
    }
}
