package com.example.karma.ui.theme

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import com.example.karma.R
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
    val gradientColors by remember {
        derivedStateOf {
            listOf(Color(settings.themeGradientBaseColor), Color(settings.themeGradientAccentColor))
        }
    }

    // 状态栏/导航栏图标明暗随渐变顶色自适应：顶色偏亮 → 深色图标，偏暗 → 浅色图标
    val view = LocalView.current
    LaunchedEffect(gradientColors) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val darkBackground = gradientColors.first().luminance() < 0.5f
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkBackground
            isAppearanceLightNavigationBars = !darkBackground
        }
    }

    MaterialTheme(
        colorScheme = KarmaColorScheme,
        typography = Typography,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = gradientColors)),
        ) {
            // 内容在星星图片之下
            content()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.theme_bg),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
