package com.evrenhouse.esteh.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily

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

@Composable
fun EstehTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalEstehExtraColors provides EstehExtraColors()) {
        MaterialTheme(
            colorScheme = EstehLightScheme,
            typography = EstehTypography,
            content = content,
        )
    }
}
