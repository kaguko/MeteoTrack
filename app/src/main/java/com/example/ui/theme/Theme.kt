package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6ECFB),
    onPrimaryContainer = Color(0xFF0C3A57),
    secondary = RainCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F0F6),
    onSecondaryContainer = Color(0xFF053B47),
    tertiary = WarningAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDEBC8),
    onTertiaryContainer = Color(0xFF4A2A00),
    background = Color(0xFFF4F8FB),
    onBackground = Color(0xFF0F1B26),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F1B26),
    surfaceVariant = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF41505D),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5F9),
    surfaceContainer = Color(0xFFE9F0F5),
    surfaceContainerHigh = Color(0xFFE3EBF1),
    surfaceContainerHighest = Color(0xFFDCE6ED),
    outline = Color(0xFF6B7A88),
    outlineVariant = Color(0xFFC9D3DC),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = Color(0xFF00334D),
    primaryContainer = Color(0xFF0B4A6F),
    onPrimaryContainer = Color(0xFFCDEBFF),
    secondary = Color(0xFF67D4E8),
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF0E4F5C),
    onSecondaryContainer = Color(0xFFC6EFF8),
    tertiary = WarningAmberLight,
    onTertiary = Color(0xFF3D2300),
    tertiaryContainer = Color(0xFF5A3A00),
    onTertiaryContainer = Color(0xFFFFE2A8),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFE8EEF5),
    surface = Color(0xFF111A2E),
    onSurface = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFF24304A),
    onSurfaceVariant = Color(0xFFB4C0D0),
    surfaceContainerLowest = Color(0xFF080D1A),
    surfaceContainerLow = Color(0xFF0F1729),
    surfaceContainer = Color(0xFF141E34),
    surfaceContainerHigh = Color(0xFF1B2640),
    surfaceContainerHighest = Color(0xFF222E4A),
    outline = Color(0xFF8593A6),
    outlineVariant = Color(0xFF334057),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/**
 * App theme. Dynamic (wallpaper) colour is intentionally off: the weather gradients and status
 * colours are tuned for contrast against this fixed palette.
 */
@Composable
fun MeteoTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
