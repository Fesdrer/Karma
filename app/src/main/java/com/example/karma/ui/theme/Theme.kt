package com.example.karma.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A)), // 玄天：纯黑基底
        ) {
            // 地黄：底部极暗暖色渐变（内容之下）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF1A1404), // 底部暗金辉光（暗金而非橙）
                            )
                        )
                    )
            )

            content()

            // 氛围覆盖层：天地玄黄 — 极淡暗金辉光（内容之上，不影响可读性）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,       // 顶部透明
                                Color(0x15B8860B),       // 中段 ~8% 暗金
                                Color(0x30B8860B),       // 底部 ~19% 暖土金
                            )
                        )
                    )
            )
        }
    }
}
