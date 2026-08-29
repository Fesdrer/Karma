package com.example.karma.ui.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.ScoreBtnBg
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PADDING_FRACTION = 0.10f

@Composable
fun ScorePanel(
    scoreFlow: StateFlow<Float>,
    onScoreSelected: (Float) -> Unit,
    onCustomScoreChanged: (String) -> Unit,
    axisFontSize: Float = 22f,
    axisRangeMin: Float = -6f,
    axisRangeMax: Float = 6f,
    modifier: Modifier = Modifier,
) {
    val selectedScore by scoreFlow.collectAsState()
    Column(
        modifier = modifier
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
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
        val displayScore = selectedScore
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
                selectedScore = selectedScore,
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
            placeholder = {
                // 面板变窄后放不下「自定义分数...」，缩短并强制单行省略，避免占位符换行导致高度跳动
                Text(
                    "分数...",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
    // Pre-allocated Paints for tick labels (avoid per-frame allocation in Canvas)
    val leftLabelPaint = remember {
        android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.LEFT }
    }
    val rightLabelPaint = remember {
        android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.RIGHT }
    }

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
                // 拖动中实时吸附到最近的整数或 .5 刻度（只在刻度位置停留），
                // 松手/取消时再吸附一次，确保最终落在刻度上。
                var lastRawScore = selectedScore
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        lastRawScore = snapToHalf(
                            yToScore(offset.y, size.height.toFloat(), axisRangeMin, axisRangeMax)
                                .coerceIn(axisRangeMin, axisRangeMax)
                        )
                    },
                    onVerticalDrag = { change, _ ->
                        val score = yToScore(change.position.y, size.height.toFloat(), axisRangeMin, axisRangeMax)
                            .coerceIn(axisRangeMin, axisRangeMax)
                        lastRawScore = score
                        onScoreSelected(snapToHalf(score).coerceIn(axisRangeMin, axisRangeMax))
                        change.consume()
                    },
                    onDragEnd = {
                        onScoreSelected(snapToHalf(lastRawScore).coerceIn(axisRangeMin, axisRangeMax))
                    },
                    onDragCancel = {
                        onScoreSelected(snapToHalf(lastRawScore).coerceIn(axisRangeMin, axisRangeMax))
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

        // Axis X position — 居中（50%），与上方居中的「当前加减分」数字垂直对齐。
        // 整数标签在右、半格标签在左，两侧各留半宽，默认范围 ±6 时标签宽度足够。
        val axisX = w * 0.5f

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
                leftLabelPaint.apply {
                    color = android.graphics.Color.argb(
                        (alpha * 255).toInt(),
                        (labelColor.red * 255).toInt(),
                        (labelColor.green * 255).toInt(),
                        (labelColor.blue * 255).toInt(),
                    )
                    textSize = labelSize
                    isFakeBoldText = tickValue == 0f
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX + 6f * densityFactor,
                    y + labelSize * 0.35f,
                    leftLabelPaint,
                )
            } else {
                // Half label: on the left
                rightLabelPaint.apply {
                    color = android.graphics.Color.argb(
                        (alpha * 255).toInt(),
                        (labelColor.red * 255).toInt(),
                        (labelColor.green * 255).toInt(),
                        (labelColor.blue * 255).toInt(),
                    )
                    textSize = labelSize
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    axisX - 6f * densityFactor,
                    y + labelSize * 0.35f,
                    rightLabelPaint,
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
