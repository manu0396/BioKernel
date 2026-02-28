package com.neurogenesis.components.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BioKernelTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = BioKernelColors.BioTeal,
        onPrimary = BioKernelColors.DeepNavy,
        surface = BioKernelColors.LighterNavy,
        background = BioKernelColors.DeepNavy,
        onSurface = BioKernelColors.BioWhite,
        onBackground = BioKernelColors.BioWhite
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

