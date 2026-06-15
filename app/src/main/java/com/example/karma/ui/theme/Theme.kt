package com.example.karma.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KarmaColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DarkNavy,
    primaryContainer = AccentBg,
    onPrimaryContainer = Gold,
    secondary = BluePrimary,
    onSecondary = TextPrimary,
    secondaryContainer = AccentBg,
    onSecondaryContainer = BlueLight,
    tertiary = BlueLight,
    onTertiary = DarkNavy,
    background = DarkNavy,
    onBackground = TextPrimary,
    surface = PanelBg,
    onSurface = TextPrimary,
    surfaceVariant = AccentBg,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = RedNegative,
    onError = TextPrimary,
)

@Composable
fun KarmaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KarmaColorScheme,
        typography = Typography,
        content = content
    )
}
