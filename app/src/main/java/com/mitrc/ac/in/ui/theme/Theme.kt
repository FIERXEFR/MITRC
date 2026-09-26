package com.mitrc.ac.`in`.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = SurfaceWhite,
    primaryContainer = NavySoft,
    onPrimaryContainer = SurfaceWhite,
    secondary = Gold,
    onSecondary = NavyDeep,
    secondaryContainer = GoldLight,
    onSecondaryContainer = NavyDeep,
    background = Slate,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = DividerSoft,
    onSurfaceVariant = TextSecondary,
    outline = DividerSoft,
    outlineVariant = DividerSoft,
    error = ErrorRed,
    onError = SurfaceWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = NavyDeep,
    primaryContainer = NavySoft,
    onPrimaryContainer = SurfaceWhite,
    secondary = GoldLight,
    onSecondary = NavyDeep,
    background = DarkBackground,
    onBackground = SurfaceWhite,
    surface = DarkSurface,
    onSurface = SurfaceWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = ErrorRed,
    onError = SurfaceWhite
)

@Composable
fun MITRCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
