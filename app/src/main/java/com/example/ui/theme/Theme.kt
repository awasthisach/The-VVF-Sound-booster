package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FlameOrange,
    secondary = WaveCyan,
    tertiary = NatureGreen,
    background = SpaceBackground,
    surface = SpaceSurface,
    onPrimary = Color.White,
    onSecondary = Color(0xFF060814),
    onTertiary = Color(0xFF060814),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF161A33),
    onSurfaceVariant = TextSecondary,
    outline = BorderMuted
)

// Fallback Light scheme centered on identical high-contrast brand alignments
private val LightColorScheme = lightColorScheme(
    primary = FlameOrange,
    secondary = WaveCyan,
    tertiary = NatureGreen,
    background = Color(0xFFF4F5FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color(0xFF060814),
    onTertiary = Color(0xFF060814),
    onBackground = Color(0xFF0D0E1A),
    onSurface = Color(0xFF0D0E1A),
    outline = Color(0xFFD2D5E0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to respect custom logo-extracted theme strictly
    content: @Composable () -> Unit,
) {
    // We strictly use our design-crafted color Schemes matching Vishva Vijayaa Foundation
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Force dark theme for visual premium audio fidelity

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
