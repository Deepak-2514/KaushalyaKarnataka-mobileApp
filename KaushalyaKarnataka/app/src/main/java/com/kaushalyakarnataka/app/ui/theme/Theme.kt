package com.kaushalyakarnataka.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Bg = Color(0xFF0B0F1A)
private val Purple = Color(0xFFA855F7)
private val Pink = Color(0xFFE879F9)
private val Slate = Color(0xFFCBD5E1)

private val KaushalyaDark = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Pink,
    onSecondary = Color.Black,
    tertiary = Color(0xFF6366F1),
    background = Bg,
    surface = Color(0xFF140B26),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF21153A),
    outline = Color(0x66A855F7),
)

@Composable
fun KaushalyaTheme(content: @Composable () -> Unit) {
    // Force elegant dark UI consistent with the web app.
    MaterialTheme(
        colorScheme = KaushalyaDark,
        typography = KaushalyaTypography,
        content = content,
    )
}

object KaushalyaColors {
    val Background = Bg
    val GlassOutline = Color(0x33A855F7)
    val GlassSurface = Color(0x6621173B)
    val Success = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Muted = Slate
}
