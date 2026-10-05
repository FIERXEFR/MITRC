package com.mitrc.ac.`in`.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

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
    val density = LocalDensity.current

    // Pin fontScale to 1.0 so 16.sp always renders as 16.sp regardless of the device's
    // accessibility font-size setting. Without this the same layout measures differently on
    // every phone and the fixed-height chips, pills and headers overflow or truncate.
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1f
        )
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
