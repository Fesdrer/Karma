package com.example.karma.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * 弹窗入场容器（Apple 材质化原则——materialize, don't just fade）：
 * 遮罩淡入（200ms），内容从 0.92 弹簧放大（damping 0.8，轻微过冲，卡片读起来像「真实材质到达」）。
 * 遮罩与卡片分层动画：遮罩只淡入不缩放，卡片 scale + alpha 同时到位。
 * maskAlpha = 0f 时无遮罩（纯内容入场，如祈福表单）。
 */
@Composable
fun DialogEntranceContainer(
    modifier: Modifier = Modifier,
    maskAlpha: Float = 0.5f,
    content: @Composable () -> Unit,
) {
    val mask = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.92f) }
    val cardAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { mask.animateTo(maskAlpha, animationSpec = tween(200)) }
        launch { cardAlpha.animateTo(1f, animationSpec = tween(150)) }
        launch {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = mask.value)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
                alpha = cardAlpha.value
            },
        ) {
            content()
        }
    }
}
