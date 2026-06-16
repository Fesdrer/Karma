package com.example.karma.ui.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.karma.data.model.Rank
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign

private const val ANIM_DURATION = 400
private val NEG_BG = Color(0xFF2d2d2d)
private val CHART_BG = Color(0xFF0d1b2a)

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
    rankColorList: List<Long> = emptyList(),
    guideLineWidth: Float = 6f,
    guideLineColor: Long = 0xFFFFD700L,
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

        // ---- 1. Background ----
        drawRect(color = CHART_BG, size = size)

        // ---- 2. Negative region ----
        val zeroY = scoreToY(0f, centerScore, halfH, h, displayRange, quarterValue)
        if (centerScore - displayRange < 0) {
            val negY = zeroY.coerceIn(0f, h)
            if (h > negY) {
                drawRect(
                    color = NEG_BG,
                    topLeft = Offset(0f, negY),
                    size = androidx.compose.ui.geometry.Size(w, h - negY),
                )
            }
        }

        // ---- 3. Rank bands ----
        val minVis = centerScore - displayRange
        val maxVis = centerScore + displayRange
        for (rank in Rank.RANKS) {
            val bt = maxOf(rank.min, minVis)
            val bb = minOf(rank.max, maxVis)
            if (bt >= bb) continue
            val yT = scoreToY(bb, centerScore, halfH, h, displayRange, quarterValue)
            val yB = scoreToY(bt, centerScore, halfH, h, displayRange, quarterValue)
            val y0 = maxOf(0f, minOf(yT, yB))
            val y1 = minOf(h, maxOf(yT, yB))
            if (y1 <= y0) continue
            val bandColor = if (rank.level - 1 in rankColorList.indices && rankColorList.isNotEmpty()) {
                Color(rankColorList[rank.level - 1])
            } else {
                Color(rank.colorHex)
            }
            drawRect(
                color = bandColor,
                topLeft = Offset(0f, y0),
                size = androidx.compose.ui.geometry.Size(w, y1 - y0),
            )
        }

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
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX - tickMajorLen - 3f,
                    y + 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (labelAlpha * 255).toInt(),
                            (labelColorValue.red * 255).toInt(),
                            (labelColorValue.green * 255).toInt(),
                            (labelColorValue.blue * 255).toInt(),
                        )
                        textSize = labelFontSize
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
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
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        axisX - 7f - 3f,
                        y + 4f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                (labelAlpha * 255).toInt(),
                                (labelColorValue.red * 255).toInt(),
                                (labelColorValue.green * 255).toInt(),
                                (labelColorValue.blue * 255).toInt(),
                            )
                            textSize = labelFontSize
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
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

        // ---- 6. Pointer at center ----
        val ptrY = h / 2f

        // Dashed guide line
        drawLine(
            color = Color(guideLineColor),
            start = Offset(0f, ptrY),
            end = Offset(w, ptrY),
            strokeWidth = guideLineWidth,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(8f, 6f), 0f
            ),
        )

        // Triangle pointer at right edge (bigger, to be visible above the guide line)
        val triSize = maxOf(guideLineWidth * 3f, 22f)
        val triPath = Path().apply {
            moveTo(w - 3f, ptrY)
            lineTo(w - 3f - triSize, ptrY - triSize / 2f)
            lineTo(w - 3f - triSize, ptrY + triSize / 2f)
            close()
        }
        drawPath(triPath, color = Color(guideLineColor), style = Fill)

        // Score label (above the guide line so it's not covered)
        val scoreLabel = if (centerScore.toInt().toFloat() == centerScore) {
            centerScore.toInt().toString()
        } else {
            String.format("%.1f", centerScore)
        }
        drawContext.canvas.nativeCanvas.drawText(
            scoreLabel,
            w - 16f,
            ptrY - 10f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(255, 215, 0)
                textSize = 21f
                textAlign = android.graphics.Paint.Align.RIGHT
                isFakeBoldText = true
            }
        )

        // ---- 7. Range labels ----
        val topScore = centerScore + displayRange
        val botScore = centerScore - displayRange
        val topLabel = String.format("%.1f", topScore).replace(".0", "")
        val botLabel = String.format("%.1f", botScore).replace(".0", "")

        val rangePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(64, 255, 255, 255)
            textSize = 16f
            textAlign = android.graphics.Paint.Align.RIGHT
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
