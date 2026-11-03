package com.rebuild.tennisai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Палитра клона — снята со скриншотов оригинала (docs/CLONE_SPEC.md §1.1)
val Bg = Color(0xFF050505)
val Bar = Color(0xFF0A0A0A)
val Hairline = Color(0xFF1F1F22)
val CardBg = Color(0xFF141416)
val CardBorder = Color(0xFF26262A)
val CardAccentBg = Color(0xFF131C0B)
val CardAccentBorder = Color(0xFF3F5A22)
val Accent = Color(0xFF9ACD32)
val AccentDeep = Color(0xFF7CB342)
val AccentSoft = Color(0xFFC6E36A)
val TextPrimary = Color(0xFFF2F2F2)
val TextSecondary = Color(0xFF8E8E93)
val TextDim = Color(0xFF5A5A5E)
val ServeBlue = Color(0xFF4DA3FF)
val BackhandAmber = Color(0xFFF5A623)
val DrillsRed = Color(0xFFE5544B)
val CoachOrange = Color(0xFFF0793D)
val StopRed = Color(0xFFEB4B3B)
val HoloTeal = Color(0xFF5CE8C8)

// Алиасы для экранов анализа (Results/Live/Analyze)
val RedFault = StopRed
val GreenGood = Color(0xFF3FD97F)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF101400),
    secondary = AccentDeep,
    background = Bg,
    surface = CardBg,
    surfaceVariant = CardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorder,
    error = StopRed,
)

val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

// Три регистра типографики оригинала (CLONE_SPEC §1.2)
val SerifDisplay = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    letterSpacing = 1.5.sp,
)
val MonoLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    letterSpacing = 3.sp,
)
val MonoSmall = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 2.sp,
)

@Composable
fun TennisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = AppTypography,
        content = content,
    )
}
