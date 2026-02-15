package com.neurogenesis.components.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.neurogenesis.components.theme.BioKernelColors

@Composable
fun BioKernelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BioKernelColors.GlassBorder,
                        BioKernelColors.GlassBorder.copy(alpha = 0f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        color = BioKernelColors.GlassSurface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
