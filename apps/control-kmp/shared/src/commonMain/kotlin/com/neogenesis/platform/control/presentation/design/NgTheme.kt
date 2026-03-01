package com.neogenesis.platform.control.presentation.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

// --- Tokens ---

object NgColors {
    val Primary = Color(0xFF0B3D91) // Deep Tech Blue
    val Secondary = Color(0xFF00B3C6) // Cyan
    val Tertiary = Color(0xFF1C6E5D) // Teal Green
    val Success = Color(0xFF1B9C61)
    val Warning = Color(0xFFE38B2C)
    val Error = Color(0xFFE45757)
    val Info = Color(0xFF3A7BD5)

    val LightSurface = Color(0xFFF6F8FB)
    val LightOnSurface = Color(0xFF101214)
    val DarkSurface = Color(0xFF0C0F14)
    val DarkOnSurface = Color(0xFFE6E8EC)
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

private val NgTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.6.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun NgTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NgTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp)
        ),
        content = content
    )
}
