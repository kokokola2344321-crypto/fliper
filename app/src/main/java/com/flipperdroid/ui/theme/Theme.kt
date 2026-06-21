package com.flipperdroid.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FlipperColors {
    val Background = Color(0xFF111111)
    val Surface = Color(0xFF1A1A1A)
    val Primary = Color(0xFF00FF00)
    val Secondary = Color(0xFF00CC00)
    val Accent = Color(0xFF33FF33)
    val TextPrimary = Color(0xFF00FF00)
    val TextSecondary = Color(0xFF008800)
    val TextDim = Color(0xFF005500)
    val Error = Color(0xFFFF3333)
    val Warning = Color(0xFFFFAA00)
    val Info = Color(0xFF33AAFF)
    val Selected = Color(0xFF003300)
    val Border = Color(0xFF00FF00)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
}

private val DarkColorScheme = darkColorScheme(
    primary = FlipperColors.Primary,
    secondary = FlipperColors.Secondary,
    tertiary = FlipperColors.Accent,
    background = FlipperColors.Background,
    surface = FlipperColors.Surface,
    onPrimary = FlipperColors.Black,
    onSecondary = FlipperColors.Black,
    onBackground = FlipperColors.TextPrimary,
    onSurface = FlipperColors.TextPrimary,
    error = FlipperColors.Error,
)

@Composable
fun FlipperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}