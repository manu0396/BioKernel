package com.neogenesis.platform.control.presentation.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// --- Tokens ---

object NgColors {
    val Primary = Color(0xFF3F51B5) // Deep Indigo
    val Secondary = Color(0xFF009688) // Teal
    val Tertiary = Color(0xFFE91E63) // Pink accent
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)

    val LightSurface = Color(0xFFF8F9FA)
    val LightOnSurface = Color(0xFF1A1C1E)
    val DarkSurface = Color(0xFF121416)
    val DarkOnSurface = Color(0xFFE2E2E6)
}

object NgSpacing {
    val None = 0.dp
    val Tiny = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
}

// --- Theme ---

private val LightColorScheme = lightColorScheme(
    primary = NgColors.Primary,
    secondary = NgColors.Secondary,
    tertiary = NgColors.Tertiary,
    surface = NgColors.LightSurface,
    onSurface = NgColors.LightOnSurface,
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    error = NgColors.Error
)

private val DarkColorScheme = darkColorScheme(
    primary = NgColors.Primary,
    secondary = NgColors.Secondary,
    tertiary = NgColors.Tertiary,
    surface = NgColors.DarkSurface,
    onSurface = NgColors.DarkOnSurface,
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    error = NgColors.Error
)

@Composable
fun NgTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
