package com.example.karma.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 按压反馈（Apple 响应性原则）：
 * 按下瞬间快速缩到 0.95（90ms，tween 无延迟），松开用弹簧回弹到 1.0。
 * 弹簧天然可中断：快速连按时每次都以当前值继续动画，不会跳变。
 * transform-only 动画，不触发布局。
 *
 * 用法：与 clickable 共用同一个 interactionSource。
 */
@Composable
fun Modifier.pressFeedback(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            scale.animateTo(pressedScale, animationSpec = tween(90))
        } else {
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
        }
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
