package com.neogenesis.platform.control

import com.neogenesis.platform.control.presentation.CommercialOpportunity
import com.neogenesis.platform.control.presentation.CommercialPipeline
import kotlin.test.Test
import kotlin.test.assertEquals

class CommercialPipelineTest {
    @Test
    fun pipelineHoldsStages() {
        val opp = CommercialOpportunity(
            id = "1",
            name = "Test",
            stage = "Lead",
            expectedRevenueEur = 1000.0,
            probability = 50,
            notes = "",
            loiSigned = false
        )
        val pipeline = CommercialPipeline(mapOf("Lead" to listOf(opp)))
        assertEquals(1, pipeline.stages["Lead"]?.size)
    }
}
