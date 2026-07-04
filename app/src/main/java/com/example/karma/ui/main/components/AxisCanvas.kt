package com.example.karma.ui.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.karma.data.model.Rank
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign

private const val ANIM_DURATION = 400

@Composable
fun AxisCanvas(
    totalScore: Float,
    labelColor: Long = 0x80FFFFFF.toLong(),
    tickThickness: Float = 1f,
    labelFontSize: Float = 19f,
    displayRange: Float = 100f,
    showNearby: Boolean = true,
    nearbyRange: Float = 10f,
    quarterValue: Float = 30f,
    ranks: List<com.example.karma.data.model.Rank> = emptyList(),
    dotColor: Long = 0xFFFF0000L,
    modifier: Modifier = Modifier,
) {
    // Animate the score value
    val animatedScore by animateFloatAsState(
        targetValue = totalScore,
        animationSpec = tween(
            durationMillis = ANIM_DURATION,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "scoreAnim",
    )

    // Pre-allocate Paint objects (created once, mutated per-frame inside Canvas)
    val labelPaint = remember {
        android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.RIGHT }
    }
    val rangePaint = remember {
        android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.RIGHT }
    }

    Canvas(
        modifier = modifier
            .width(75.dp)
            .fillMaxHeight(),
    ) {
        val w = size.width
        val h = size.height
        val halfH = h / 2f
        val axisX = w / 2f
        val centerScore = animatedScore

        if (w <= 0 || h <= 0) return@Canvas

        val labelColorValue = Color(labelColor)
        val labelAlpha = labelColorValue.alpha

        // ---- 1. Rank bands with horizontal gradient + vertical DstIn fade ----
        val minVis = centerScore - displayRange
        val maxVis = centerScore + displayRange

        for (rank in ranks) {
            val bt = maxOf(rank.min, minVis)
            val bb = minOf(rank.max, maxVis)
            if (bt >= bb) continue
            val yT = scoreToY(bb, centerScore, halfH, h, displayRange, quarterValue)
            val yB = scoreToY(bt, centerScore, halfH, h, displayRange, quarterValue)
            val y0 = maxOf(0f, minOf(yT, yB))
            val y1 = minOf(h, maxOf(yT, yB))
            if (y1 <= y0) continue
            val rankColor = Color(rank.colorHex)
            val bandH = y1 - y0

            // 单条 rect 横跨全宽：
            // 0%→30%: 渐变透明→不透明 | 30%→70%: 实色 | 70%→100%: 渐变不透明→透明
            // 用 rankColor.copy(alpha=0f) 替代 Color.Transparent，保持 RGB 不变、仅变 alpha
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to rankColor.copy(alpha = 0f),  // 左边缘，RGB=rankColor，alpha=0
                        0.3f to rankColor,                   // 30%处完全不透明
                        0.7f to rankColor,                   // 70%处仍不透明
                        1f to rankColor.copy(alpha = 0f),   // 右边缘，RGB=rankColor，alpha=0
                    ),
                ),
                topLeft = Offset(0f, y0),
                size = androidx.compose.ui.geometry.Size(w, bandH),
            )
        }

        // ---- Vertical fade: 无叠加层，无 BlendMode ----
        // rank 色带自然终止于其 y 边界，水平渐变提供左右软边缘，
        // 上下方向直接透明到主题渐变色背景。

        // ---- 4. Ticks ----
        val idealTicks = 16
        val rawInterval = (displayRange * 2) / idealTicks
        val niceIntervals = listOf(1f, 2f, 5f, 10f, 20f, 50f)
        val tickInterval = niceIntervals.minByOrNull { abs(it - rawInterval) } ?: 1f
        val firstTick = kotlin.math.ceil(minVis / tickInterval) * tickInterval
        val lastTick = kotlin.math.floor(maxVis / tickInterval) * tickInterval
        val drawMinor = tickInterval > 1f
        val minorInterval = tickInterval / 2f

        val tickMajorLen = 7f
        val tickMinorLen = 4f

        // Major ticks
        var s = firstTick
        while (s <= lastTick + 0.001f) {
            val y = scoreToY(s, centerScore, halfH, h, displayRange, quarterValue)
            if (y in -8f..h + 8f) {
                drawLine(
                    color = labelColorValue,
                    start = Offset(axisX - tickMajorLen, y),
                    end = Offset(axisX + tickMajorLen, y),
                    strokeWidth = tickThickness,
                )
                // Label
                val label = String.format("%.1f", s).replace(".0", "")
                labelPaint.apply {
                    color = android.graphics.Color.argb(
                        (labelAlpha * 255).toInt(),
                        (labelColorValue.red * 255).toInt(),
                        (labelColorValue.green * 255).toInt(),
                        (labelColorValue.blue * 255).toInt(),
                    )
                    textSize = labelFontSize
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX - tickMajorLen - 3f,
                    y + 4f,
                    labelPaint,
                )
            }
            s += tickInterval
        }

        // Minor ticks
        if (drawMinor) {
            var sMinor = (kotlin.math.ceil(minVis / minorInterval).toInt()) * minorInterval
            while (sMinor <= maxVis) {
                if (abs(sMinor % tickInterval) > 0.001f) {
                    val y = scoreToY(sMinor, centerScore, halfH, h, displayRange, quarterValue)
                    if (y in -8f..h + 8f) {
                        drawLine(
                            color = labelColorValue.copy(alpha = 0.12f),
                            start = Offset(axisX - tickMinorLen, y),
                            end = Offset(axisX + tickMinorLen, y),
                            strokeWidth = tickThickness,
                        )
                    }
                }
                sMinor += minorInterval
            }
        }

        // ---- Even-numbered reference ticks（近邻刻度） ----
        if (showNearby && nearbyRange > 0f) {
            val evenMin = kotlin.math.ceil((centerScore - nearbyRange) / 2f).toInt() * 2
            val evenMax = kotlin.math.floor((centerScore + nearbyRange) / 2f).toInt() * 2
            var evenTick = evenMin
            while (evenTick <= evenMax) {
                val y = scoreToY(evenTick.toFloat(), centerScore, halfH, h, displayRange, quarterValue)
                if (y in -8f..h + 8f) {
                    drawLine(
                        color = labelColorValue,
                        start = Offset(axisX - 7f, y),
                        end = Offset(axisX + 7f, y),
                        strokeWidth = tickThickness,
                    )
                    val label = String.format("%.1f", evenTick.toFloat()).replace(".0", "")
                    labelPaint.apply {
                        color = android.graphics.Color.argb(
                            (labelAlpha * 255).toInt(),
                            (labelColorValue.red * 255).toInt(),
                            (labelColorValue.green * 255).toInt(),
                            (labelColorValue.blue * 255).toInt(),
                        )
                        textSize = labelFontSize
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        axisX - 7f - 3f,
                        y + 4f,
                        labelPaint,
                    )
                }
                evenTick += 2
            }
        }

        // ---- 5. Axis line ----
        drawLine(
            color = labelColorValue,
            start = Offset(axisX, 0f),
            end = Offset(axisX, h),
            strokeWidth = tickThickness,
        )

        // ---- 6. Pointer — glowing dot at center ----
        val ptrY = h / 2f

        // Glowing dot at axis intersection
        val dotCenter = Offset(axisX, ptrY)
        val dotColorValue = Color(dotColor)
        drawCircle(color = dotColorValue.copy(alpha = 0.12f), radius = 44f, center = dotCenter)
        drawCircle(color = dotColorValue.copy(alpha = 0.3f), radius = 24f, center = dotCenter)
        drawCircle(color = dotColorValue, radius = 8f, center = dotCenter)

        // ---- 7. Range labels ----
        val topScore = centerScore + displayRange
        val botScore = centerScore - displayRange
        val topLabel = String.format("%.1f", topScore).replace(".0", "")
        val botLabel = String.format("%.1f", botScore).replace(".0", "")

        rangePaint.apply {
            color = android.graphics.Color.argb(64, 255, 255, 255)
            textSize = 16f
        }
        drawContext.canvas.nativeCanvas.drawText(topLabel, axisX - tickMajorLen - 3f, 10f, rangePaint)
        drawContext.canvas.nativeCanvas.drawText(botLabel, axisX - tickMajorLen - 3f, h - 8f, rangePaint)
    }
}

/** Non-linear score → Y mapping with dynamic exponent from quarterValue. */
private fun scoreToY(
    score: Float,
    centerScore: Float,
    halfH: Float,
    canvasH: Float,
    displayRange: Float,
    quarterValue: Float,
): Float {
    val d = score - centerScore
    val sign = sign(d)
    val absD = minOf(abs(d), displayRange * 2f)

    // 从 quarterValue 动态计算 exponent
    // quarterValue: 上方 1/4 处显示的分数偏移值
    // 公式: exp = ln(0.5) / ln(quarterValue / displayRange)
    val exp = if (quarterValue > 0f && quarterValue < displayRange) {
        (ln(0.5) / ln((quarterValue / displayRange).toDouble())).toFloat()
    } else {
        1f // 线性
    }

    val scale = halfH / (displayRange.toDouble().pow(exp.toDouble())).toFloat()
    val pixelOffset = sign * (absD.toDouble().pow(exp.toDouble())).toFloat() * scale
    return canvasH / 2f - pixelOffset
}
