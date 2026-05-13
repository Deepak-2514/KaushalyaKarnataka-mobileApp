package com.kaushalyakarnataka.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Dark Palette
private val PrimaryIndigo = Color(0xFF4F46E5)
private val SecondaryPurple = Color(0xFF8B5CF6)
private val BackgroundTop = Color(0xFF0F172A)
private val BackgroundBottom = Color(0xFF111827)
private val SurfaceDark = Color(0xFF1E293B)
private val SurfaceSecondary = Color(0xFF243145)

private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFFCBD5E1)
private val TextMuted = Color(0xFF94A3B8)

private val KaushalyaDark = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    secondary = SecondaryPurple,
    onSecondary = Color.White,
    tertiary = Color(0xFFF472B6),
    background = BackgroundTop,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = Color(0x14FFFFFF), // Subtle border rgba(255,255,255,0.08)
    error = Color(0xFFEF4444)
)

@Composable
fun KaushalyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KaushalyaDark,
        typography = KaushalyaTypography,
        content = content,
    )
}

object KaushalyaColors {
    val Background = BackgroundTop
    val BackgroundGradient = listOf(BackgroundTop, BackgroundBottom)
    val GlassSurface = Color(0xCC1E293B) // rgba(30, 41, 59, 0.8)
    val GlassOutline = Color(0x14FFFFFF) // rgba(255, 255, 255, 0.08)
    val Primary = PrimaryIndigo
    val Secondary = SecondaryPurple
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Muted = TextMuted
    val NavBackground = Color(0xF20F172A) // rgba(15, 23, 42, 0.95)
}

// Extension to allow custom success/warning colors in ColorScheme if needed
val androidx.compose.material3.ColorScheme.success: Color get() = Color(0xFF10B981)
val androidx.compose.material3.ColorScheme.warning: Color get() = Color(0xFFF59E0B)
