package com.evrenhouse.esteh.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Token eksak dari web POS (android.md §5). Jangan pakai M3 baseline (ungu).
val Canvas = Color(0xFFFBFBFA)
val Subtle = Color(0xFFF7F6F3)
val Card = Color(0xFFFFFFFF)
val Line = Color(0xFFEAEAEA)
val Ink = Color(0xFF111111)
val Ink2 = Color(0xFF2F3437)
val Muted = Color(0xFF787774)

val CashContainer = Color(0xFFEDF3EC)
val OnCashContainer = Color(0xFF346538)
val QrisContainer = Color(0xFFFDEBEC)
val OnQrisContainer = Color(0xFF9F2F2D)
val InfoContainer = Color(0xFFE1F3FE)
val OnInfoContainer = Color(0xFF1F6C9F)
val WarnContainer = Color(0xFFFBF3DB)
val OnWarnContainer = Color(0xFF956400)

/** Warna pastel tambahan via CompositionLocal (di luar ColorScheme M3). */
@Immutable
data class EstehExtraColors(
    val cashContainer: Color = CashContainer,
    val onCashContainer: Color = OnCashContainer,
    val qrisContainer: Color = QrisContainer,
    val onQrisContainer: Color = OnQrisContainer,
    val infoContainer: Color = InfoContainer,
    val onInfoContainer: Color = OnInfoContainer,
    val warnContainer: Color = WarnContainer,
    val onWarnContainer: Color = OnWarnContainer,
)

val LocalEstehExtraColors = staticCompositionLocalOf { EstehExtraColors() }
