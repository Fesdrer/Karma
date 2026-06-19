package com.example.karma.ui.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ScoreBtnBg
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PADDING_FRACTION = 0.10f

@Composable
fun ScorePanel(
    selectedScore: Float?,
    onScoreSelected: (Float) -> Unit,
    onCustomScoreChanged: (String) -> Unit,
    axisFontSize: Float = 22f,
    axisRangeMin: Float = -6f,
    axisRangeMax: Float = 6f,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        // Title
        Text(
            text = "分数",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFffd700),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(4.dp))

        // ===== 当前选中分数显示（始终占位） =====
        val displayScore = selectedScore ?: 0f
        val displayText = if (displayScore % 1f == 0f) {
            (if (displayScore > 0) "+" else "") + displayScore.toInt().toString()
        } else {
            (if (displayScore > 0) "+" else "") + String.format("%.1f", displayScore)
        }
        val scoreColor = when {
            displayScore > 0f -> Color(0xFF69f0ae)
            displayScore < 0f -> Color(0xFFff5252)
            else -> Color(0xFFa0c4ff)
        }
        Text(
            text = displayText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = scoreColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        // ===== 结束选中分数显示 =====

        // Vertical axis canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ScoreAxisView(
                selectedScore = selectedScore ?: 0f,
                onScoreSelected = onScoreSelected,
                axisFontSize = axisFontSize,
                axisRangeMin = axisRangeMin,
                axisRangeMax = axisRangeMax,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(6.dp))

        // Custom input
        var customText by remember { mutableStateOf("") }
        OutlinedTextField(
            value = customText,
            onValueChange = {
                customText = it
                onCustomScoreChanged(it)
            },
            placeholder = { Text("自定义分数...", fontSize = 12.sp, color = Color(0xFF666666)) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            shape = RoundedCornerShape(7.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFffd700),
                unfocusedBorderColor = BorderSubtle,
                cursorColor = Color(0xFFffd700),
                focusedContainerColor = ScoreBtnBg,
                unfocusedContainerColor = ScoreBtnBg,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScoreAxisView(
    selectedScore: Float,
    onScoreSelected: (Float) -> Unit,
    axisFontSize: Float = 22f,
    axisRangeMin: Float = -6f,
    axisRangeMax: Float = 6f,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .pointerInput(axisRangeMin, axisRangeMax) {
                detectTapGestures { offset ->
                    val score = yToScore(offset.y, size.height.toFloat(), axisRangeMin, axisRangeMax)
                    val snapped = snapToHalf(score).coerceIn(axisRangeMin, axisRangeMax)
                    onScoreSelected(snapped)
                }
            }
            .pointerInput(axisRangeMin, axisRangeMax) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, _ ->
                        val score = yToScore(change.position.y, size.height.toFloat(), axisRangeMin, axisRangeMax)
                        val snapped = snapToHalf(score).coerceIn(axisRangeMin, axisRangeMax)
                        onScoreSelected(snapped)
                        change.consume()
                    },
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val densityFactor = density
        if (w <= 0 || h <= 0) return@Canvas

        val paddingTop = h * PADDING_FRACTION
        val paddingBottom = h * PADDING_FRACTION
        val usableH = h - paddingTop - paddingBottom
        val baseSize = axisFontSize * densityFactor

        // Axis X position — left-aligned so labels fit on the right
        val axisX = w * 0.35f

        // ---- Background ----
        drawRect(color = Color(0xFF0d1b2a).copy(alpha = 0.3f), size = size)

        // ---- Axis vertical line ----
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(axisX, paddingTop),
            end = Offset(axisX, h - paddingBottom),
            strokeWidth = 1.5f * densityFactor,
        )

        // ---- Ticks and labels ----
        var tickValue = axisRangeMin
        while (tickValue <= axisRangeMax + 0.001f) {
            val y = scoreToAxisY(tickValue, paddingTop, usableH, axisRangeMin, axisRangeMax)
            val isInteger = (tickValue % 1f).let { abs(it) < 0.01f }
            val tickLen = if (isInteger) 8f * densityFactor else 5f * densityFactor

            // Tick line
            val labelSize = when {
                tickValue == 0f -> baseSize * 1.18f
                isInteger -> baseSize
                else -> baseSize * 0.82f
            }
            if (isInteger) {
                // Integer: line to the left
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(axisX - tickLen, y),
                    end = Offset(axisX, y),
                    strokeWidth = 1f,
                )
            } else {
                // Half: line to the right
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(axisX, y),
                    end = Offset(axisX + tickLen, y),
                    strokeWidth = 1f,
                )
            }

            // Label
            val label = when {
                tickValue == 0f -> "0"
                tickValue > 0 -> "+${formatTickValue(tickValue)}"
                else -> formatTickValue(tickValue)
            }
            val labelColor = when {
                tickValue == 0f -> Color(0xFFffd700)
                tickValue < 0 -> Color(0xFFff8a80)
                else -> Color(0xFFa0c4ff)
            }
            val alpha = if (tickValue == 0f) 1f else 0.7f

            if (isInteger) {
                // Integer label: on the right
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX + 6f * densityFactor,
                    y + labelSize * 0.35f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (alpha * 255).toInt(),
                            (labelColor.red * 255).toInt(),
                            (labelColor.green * 255).toInt(),
                            (labelColor.blue * 255).toInt(),
                        )
                        textSize = labelSize
                        textAlign = android.graphics.Paint.Align.LEFT
                        isFakeBoldText = tickValue == 0f
                    }
                )
            } else {
                // Half label: on the left
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX - 6f * densityFactor,
                    y + labelSize * 0.35f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (alpha * 255).toInt(),
                            (labelColor.red * 255).toInt(),
                            (labelColor.green * 255).toInt(),
                            (labelColor.blue * 255).toInt(),
                        )
                        textSize = labelSize
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            tickValue += 0.5f
        }

        // ---- Red selection circle ----
        val circleY = scoreToAxisY(selectedScore, paddingTop, usableH, axisRangeMin, axisRangeMax)
        val circleCenter = Offset(axisX, circleY)

        // Outer glow
        drawCircle(
            color = Color(0xFFff5252).copy(alpha = 0.2f),
            radius = 14f * densityFactor,
            center = circleCenter,
        )
        // Main red circle
        drawCircle(
            color = Color(0xFFff5252),
            radius = 8f * densityFactor,
            center = circleCenter,
        )
        // Inner white highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = 3f * densityFactor,
            center = Offset(axisX - 1.5f * densityFactor, circleY - 1.5f * densityFactor),
        )
    }
}

// ---- Math helpers ----

/** Map a score to a canvas Y coordinate. Handles asymmetric ranges. */
private fun scoreToAxisY(score: Float, paddingTop: Float, usableH: Float, axisRangeMin: Float, axisRangeMax: Float): Float {
    val mid = (axisRangeMax + axisRangeMin) / 2f
    val halfRange = (axisRangeMax - axisRangeMin) / 2f
    if (halfRange <= 0f) return paddingTop + usableH / 2f
    val ratio = ((score - mid) / halfRange).coerceIn(-1f, 1f)
    return paddingTop + usableH * (0.5f - ratio * 0.5f)
}

/** Map a canvas Y coordinate back to a score. Handles asymmetric ranges. */
private fun yToScore(y: Float, canvasH: Float, axisRangeMin: Float, axisRangeMax: Float): Float {
    val paddingTop = canvasH * PADDING_FRACTION
    val usableH = canvasH * 0.80f
    val mid = (axisRangeMax + axisRangeMin) / 2f
    val halfRange = (axisRangeMax - axisRangeMin) / 2f
    if (halfRange <= 0f) return mid
    val ratio = 1f - (y - paddingTop) / usableH * 2f
    return mid + ratio * halfRange
}

/** Snap to the nearest 0.5. */
private fun snapToHalf(v: Float): Float {
    return (v * 2f).roundToInt() / 2f
}

/** Format a tick value: remove trailing ".0" for integers. */
private fun formatTickValue(v: Float): String {
    return if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)
}
