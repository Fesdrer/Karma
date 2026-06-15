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
import kotlin.math.pow
import kotlin.math.sign

private const val AXIS_DISPLAY_RANGE = 100f
private const val ANIM_DURATION = 400
private val NEG_BG = Color(0xFF2d2d2d)
private val CHART_BG = Color(0xFF0d1b2a)

@Composable
fun AxisCanvas(
    totalScore: Float,
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

        // ---- 1. Background ----
        drawRect(color = CHART_BG, size = size)

        // ---- 2. Negative region ----
        val zeroY = scoreToY(0f, centerScore, halfH, h)
        if (centerScore - AXIS_DISPLAY_RANGE < 0) {
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
        val minVis = centerScore - AXIS_DISPLAY_RANGE
        val maxVis = centerScore + AXIS_DISPLAY_RANGE
        for (rank in Rank.RANKS) {
            val bt = maxOf(rank.min, minVis)
            val bb = minOf(rank.max, maxVis)
            if (bt >= bb) continue
            val yT = scoreToY(bb, centerScore, halfH, h)
            val yB = scoreToY(bt, centerScore, halfH, h)
            val y0 = maxOf(0f, minOf(yT, yB))
            val y1 = minOf(h, maxOf(yT, yB))
            if (y1 <= y0) continue
            drawRect(
                color = Color(rank.colorHex),
                topLeft = Offset(0f, y0),
                size = androidx.compose.ui.geometry.Size(w, y1 - y0),
            )
        }

        // ---- 4. Ticks ----
        val idealTicks = 16
        val rawInterval = (AXIS_DISPLAY_RANGE * 2) / idealTicks
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
            val y = scoreToY(s, centerScore, halfH, h)
            if (y in -8f..h + 8f) {
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(axisX - tickMajorLen, y),
                    end = Offset(axisX + tickMajorLen, y),
                    strokeWidth = 1f,
                )
                // Label - use simple formatting
                val label = String.format("%.1f", s).replace(".0", "")
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX - tickMajorLen - 3f,
                    y + 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(128, 255, 255, 255)
                        textSize = 24f
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
                    val y = scoreToY(sMinor, centerScore, halfH, h)
                    if (y in -8f..h + 8f) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(axisX - tickMinorLen, y),
                            end = Offset(axisX + tickMinorLen, y),
                            strokeWidth = 1f,
                        )
                    }
                }
                sMinor += minorInterval
            }
        }

        // ---- 5. Axis line ----
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(axisX, 0f),
            end = Offset(axisX, h),
            strokeWidth = 1f,
        )

        // ---- 6. Pointer at center ----
        val ptrY = h / 2f

        // Dashed guide line
        drawLine(
            color = Color(0xFFffd700).copy(alpha = 0.5f),
            start = Offset(0f, ptrY),
            end = Offset(w, ptrY),
            strokeWidth = 1.5f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                floatArrayOf(6f, 4f), 0f
            ),
        )

        // Triangle pointer at right edge
        val triPath = Path().apply {
            moveTo(w - 3f, ptrY)
            lineTo(w - 13f, ptrY - 6f)
            lineTo(w - 13f, ptrY + 6f)
            close()
        }
        drawPath(triPath, color = Color(0xFFffd700), style = Fill)

        // Score label
        val scoreLabel = if (centerScore.toInt().toFloat() == centerScore) {
            centerScore.toInt().toString()
        } else {
            String.format("%.1f", centerScore)
        }
        drawContext.canvas.nativeCanvas.drawText(
            scoreLabel,
            w - 16f,
            ptrY + 4f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(255, 215, 0)
                textSize = 26f
                textAlign = android.graphics.Paint.Align.RIGHT
                isFakeBoldText = true
            }
        )

        // ---- 7. Range labels ----
        val topScore = centerScore + AXIS_DISPLAY_RANGE
        val botScore = centerScore - AXIS_DISPLAY_RANGE
        val topLabel = String.format("%.1f", topScore).replace(".0", "")
        val botLabel = String.format("%.1f", botScore).replace(".0", "")

        val rangePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(64, 255, 255, 255)
            textSize = 20f
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(topLabel, axisX - tickMajorLen - 3f, 10f, rangePaint)
        drawContext.canvas.nativeCanvas.drawText(botLabel, axisX - tickMajorLen - 3f, h - 8f, rangePaint)
    }
}

// Non-linear score → Y mapping matching HTML exactly
private fun scoreToY(
    score: Float,
    centerScore: Float,
    halfH: Float,
    canvasH: Float,
): Float {
    val d = score - centerScore
    val sign = sign(d)
    val absD = minOf(abs(d), AXIS_DISPLAY_RANGE * 2f)
    val scale = halfH / (AXIS_DISPLAY_RANGE.toDouble().pow(0.6)).toFloat()
    val pixelOffset = sign * (absD.toDouble().pow(0.6)).toFloat() * scale
    return canvasH / 2f - pixelOffset
}
