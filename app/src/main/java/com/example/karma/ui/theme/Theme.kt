package com.example.karma.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.karma.data.local.entity.KarmaSettingsEntity
import com.example.karma.data.repository.KarmaRepository

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
    repository: KarmaRepository,
    content: @Composable () -> Unit
) {
    val settings by repository.settings.collectAsState(KarmaSettingsEntity())

    MaterialTheme(
        colorScheme = KarmaColorScheme,
        typography = Typography,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(settings.themeGradientBaseColor),
                            Color(settings.themeGradientAccentColor),
                        )
                    )
                ),
        ) {
            content()
        }
    }
}
