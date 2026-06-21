package com.pause.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A calm, slightly editorial palette: soft off-white canvases, deep ink text, and a single
// restrained indigo-violet accent. No bright primaries, no high-saturation surfaces.

internal val Violet = Color(0xFF5C53C9)
internal val VioletLight = Color(0xFFC4BCFF)

val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7E3FF),
    onPrimaryContainer = Color(0xFF170B5E),
    secondary = Color(0xFF615C73),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E4F1),
    onSecondaryContainer = Color(0xFF1D1A2B),
    tertiary = Color(0xFF49687E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF1A1826),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1826),
    surfaceVariant = Color(0xFFEBE8F3),
    onSurfaceVariant = Color(0xFF49475A),
    outline = Color(0xFFCAC6D6),
    outlineVariant = Color(0xFFE3E0EC),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
)

val DarkColors = darkColorScheme(
    primary = VioletLight,
    onPrimary = Color(0xFF2A2078),
    primaryContainer = Color(0xFF433A90),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = Color(0xFFCAC4DA),
    onSecondary = Color(0xFF322E40),
    secondaryContainer = Color(0xFF494657),
    onSecondaryContainer = Color(0xFFE8E4F1),
    tertiary = Color(0xFFB1CBE3),
    onTertiary = Color(0xFF193349),
    background = Color(0xFF0E0D15),
    onBackground = Color(0xFFE9E7F2),
    surface = Color(0xFF16141F),
    onSurface = Color(0xFFE9E7F2),
    surfaceVariant = Color(0xFF2A2738),
    onSurfaceVariant = Color(0xFFC9C5D6),
    outline = Color(0xFF565467),
    outlineVariant = Color(0xFF2F2C3D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)
