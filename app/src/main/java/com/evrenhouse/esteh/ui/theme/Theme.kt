package com.evrenhouse.esteh.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

// Radius 6/8/12/9999 (android.md §5). Pill via CircleShape di call-site.

private val EstehDarkScheme = darkColorScheme(
    primary = Canvas,
    onPrimary = Ink,
    surface = Color(0xFF1C1B19),
    onSurface = Color(0xFFF5F4F0),
    surfaceContainerLowest = Color(0xFF121110),
    surfaceContainerLow = Color(0xFF262522),
    surfaceContainer = Color(0xFF232220),
    surfaceContainerHigh = Color(0xFF2D2C29),
    onSurfaceVariant = Color(0xFFB9B6B0),
    outlineVariant = Color(0xFF3A3936),
    secondaryContainer = Color(0xFF2D2C29),
    onSecondaryContainer = Color(0xFFF5F4F0),
)

private val EstehLightScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Card,
    surface = Canvas,
    onSurface = Ink,
    surfaceContainerLowest = Card,
    surfaceContainerLow = Subtle,
    surfaceContainer = Card,
    surfaceContainerHigh = Subtle,
    onSurfaceVariant = Muted,
    outlineVariant = Line,
    secondaryContainer = Subtle,
    onSecondaryContainer = Ink2,
)

// Sistem saja: sans + mono (android.md §1). Tanpa bundel font.
private val EstehTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.Monospace),
)

private val EstehShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun EstehTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEstehExtraColors provides EstehExtraColors()) {
        MaterialTheme(
            colorScheme = if (dark) EstehDarkScheme else EstehLightScheme,
            typography = EstehTypography,
            shapes = EstehShapes,
            content = content,
        )
    }
}
