package com.kaushalyakarnataka.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SaaS Premium Dark Palette
private val BackgroundMain = Color(0xFF111827)
private val BackgroundSecondary = Color(0xFF1A2234)
private val SurfaceCard = Color(0xFF1E293B)
private val SurfaceElevated = Color(0xFF273449)

private val PrimaryAccent = Color(0xFF6366F1)
private val PrimaryHover = Color(0xFF818CF8)

private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val TextMuted = Color(0xFF64748B)

private val BorderSoft = Color(0x0FFFFFFF) // rgba(255,255,255,0.06)

private val KaushalyaSaaSDark = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = BackgroundSecondary,
    onSecondary = TextPrimary,
    tertiary = Color(0xFF10B981),
    background = BackgroundMain,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSoft,
    error = Color(0xFFEF4444)
)

@Composable
fun KaushalyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KaushalyaSaaSDark,
        typography = KaushalyaTypography,
        content = content,
    )
}

object KaushalyaColors {
    val Primary = PrimaryAccent
    val Secondary = BackgroundSecondary
    val Background = BackgroundMain
    val Surface = SurfaceCard
    val Elevated = SurfaceElevated
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val Border = BorderSoft
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    
    val NavBackground = Color(0xFF182235)
    val SelectedTab = PrimaryAccent
    val UnselectedTab = TextSecondary
    
    // Legacy support
    val BackgroundGradient = listOf(BackgroundMain, BackgroundMain)
    val GlassSurface = SurfaceCard
    val GlassOutline = BorderSoft
}

val androidx.compose.material3.ColorScheme.success: Color get() = Color(0xFF10B981)
val androidx.compose.material3.ColorScheme.warning: Color get() = Color(0xFFF59E0B)
val androidx.compose.material3.ColorScheme.muted: Color get() = Color(0xFF64748B)
val androidx.compose.material3.ColorScheme.border: Color get() = Color(0x0FFFFFFF)
