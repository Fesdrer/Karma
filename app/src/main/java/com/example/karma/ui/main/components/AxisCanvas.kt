package com.example.karma.ui.main.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.example.karma.data.model.Rank
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sign

/** 自证倒计时格式：HH:MM:SS（小时可超 24，如 47:59:59）。 */
private fun formatProofRemaining(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

private const val ANIM_DURATION = 400

/**
 * 阶位色带横向渐变采样点（位置, alpha），与具体颜色无关，跨帧复用。
 * 左段 0→0.3 与右段 0.7→1 各用 RAMP_STOPS 个 smoothstep 采样点；
 * 中间 0.3→0.7 不采样——相邻 stop (0.3,1) 与 (0.7,1) 之间线性插值恒为 1（实色）。
 *
 * 为什么要 smoothstep：线性渐变的 alpha 曲线在 30%/70% 拐点处斜率突变，
 * 人眼侧抑制会把突变处放大成隐约的"微亮"带（马赫带错觉）。
 * smoothstep 在端点处斜率（导数）为 0，与中间实色段的斜率 0 平滑衔接，
 * 整条 alpha 曲线导数连续，拐点不再产生亮带。
 */
private const val RAMP_STOPS = 8
private val BAND_RAMP: List<Pair<Float, Float>> = buildList {
    for (i in 0..RAMP_STOPS) {
        val t = i.toFloat() / RAMP_STOPS
        val a = t * t * (3f - 2f * t)  // smoothstep(t)：0→1，两端导数 0
        add(0.3f * t to a)
    }
    for (i in 0..RAMP_STOPS) {
        val t = i.toFloat() / RAMP_STOPS
        val a = 1f - t * t * (3f - 2f * t)  // 1 - smoothstep(t)：1→0，两端导数 0
        add(0.7f + 0.3f * t to a)
    }
}

/**
 * 垂直蒙版采样点（位置 0→1，白色 + alpha），与具体内容无关，跨帧复用。
 * 顶部 0→0.15 与底部 0.85→1 用 smoothstep 从透明升到不透明（再对称降回），
 * 中间 0.15→0.85 不采样——相邻 stop (0.15,1) 与 (0.85,1) 线性插值恒为 1。
 *
 * 配合 BlendMode.DstIn 使用：DstIn 只取蒙版的 alpha 通道，
 * 把整个数轴长条（色块/刻度/标签/轴线）的上下两端虚化，中间保持清晰；
 * 与水平渐变一样用 smoothstep，避免 15%/85% 交界处出现马赫带。
 */
private const val VERTICAL_FADE_FRACTION = 0.15f  // 顶部/底部各虚化画布高度的 15%
private const val VERTICAL_FADE_STEPS = 8
private val VERTICAL_FADE_STOPS: Array<Pair<Float, Color>> = buildList {
    // 顶部：位置 0→0.15，alpha 0→1（smoothstep）
    for (i in 0..VERTICAL_FADE_STEPS) {
        val u = i.toFloat() / VERTICAL_FADE_STEPS
        val a = u * u * (3f - 2f * u)
        add(VERTICAL_FADE_FRACTION * u to Color.White.copy(alpha = a))
    }
    // 底部：位置 0.85→1（升序！），alpha 1→0（从 0.85 处的 1 平滑降到下边缘 0）
    for (i in 0..VERTICAL_FADE_STEPS) {
        val u = i.toFloat() / VERTICAL_FADE_STEPS
        val a = 1f - u * u * (3f - 2f * u)
        add(0.85f + VERTICAL_FADE_FRACTION * u to Color.White.copy(alpha = a))
    }
}.toTypedArray()

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
    // 阶位自证特效（v4.0）
    proofActive: Boolean = false,
    proofLineColor: Long = 0xFFFFD700L,
    proofGlowColor: Long = 0xFFFFFFFFL,
    proofCountdownBg: Long = 0xFF8B0000L,
    proofCountdownText: Long = 0xFFFFFFFFL,
    proofEndTime: Long = 0L,
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

    // 自证特效：光晕粒子循环动画（进度 0→1 无限重复）
    val glowTransition = rememberInfiniteTransition(label = "proofGlow")
    val glowProgress by glowTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "proofGlowProgress",
    )
    // 倒计时剩余毫秒（每秒更新，触发重绘）
    var remainingMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(proofActive, proofEndTime) {
        if (proofActive) {
            while (true) {
                remainingMs = (proofEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Pre-allocate Paint objects (created once, mutated per-frame inside Canvas)
    val labelPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.SERIF
        }
    }
    val rangePaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.SERIF
        }
    }
    val countdownPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.SERIF
        }
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

        // ---- 上下 15% 虚化：先把整个数轴（色块/刻度/标签/轴线/光点）画进独立图层，
        // 末尾用垂直 DstIn 蒙版统一淡出上下两端；蒙版是整条长条的公共效果，
        // 无需逐元素计算，且只影响本图层，不影响页面背景。 ----
        drawIntoCanvas { canvas ->
            canvas.saveLayer(
                bounds = Rect(0f, 0f, size.width, size.height),
                paint = Paint(),
            )
        }

        val labelColorValue = Color(labelColor)
        val labelAlpha = labelColorValue.alpha

        // 预计算 exp + scale：每帧 ~80 次 scoreToY 调用共享 1 次 ln/pow
        val (exp, yScale) = precomputeExpScale(halfH, displayRange, quarterValue)

        // ---- 1. Rank bands with horizontal gradient + vertical DstIn fade ----
        val minVis = centerScore - displayRange
        val maxVis = centerScore + displayRange

        for (rank in ranks) {
            val bt = maxOf(rank.min, minVis)
            val bb = minOf(rank.max, maxVis)
            if (bt >= bb) continue
            val yT = scoreToY(bb, centerScore, h, displayRange, exp, yScale)
            val yB = scoreToY(bt, centerScore, h, displayRange, exp, yScale)
            val y0 = maxOf(0f, minOf(yT, yB))
            val y1 = minOf(h, maxOf(yT, yB))
            if (y1 <= y0) continue
            val rankColor = Color(rank.colorHex)
            val bandH = y1 - y0

            // 单条 rect 横跨全宽：
            // 0%→30%: 渐变透明→不透明 | 30%→70%: 实色 | 70%→100%: 渐变不透明→透明
            // 渐变段用 BAND_RAMP（smoothstep 采样）替代线性 4 点渐变，
            // 消除 30%/70% 拐点处的马赫带"微亮"错觉。
            // 用 rankColor.copy(alpha=...) 替代 Color.Transparent，保持 RGB 不变、仅变 alpha
            drawRect(
                brush = Brush.horizontalGradient(
                    // 展开为 vararg Pair<Float, Color>（该 Compose 版本无 List<Pair> 重载）
                    *BAND_RAMP.map { (pos, alpha) ->
                        pos to rankColor.copy(alpha = alpha)
                    }.toTypedArray(),
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
            val y = scoreToY(s, centerScore, h, displayRange, exp, yScale)
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
                    val y = scoreToY(sMinor, centerScore, h, displayRange, exp, yScale)
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
                val y = scoreToY(evenTick.toFloat(), centerScore, h, displayRange, exp, yScale)
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
        if (proofActive) {
            // 自证：金色粗线（覆盖普通轴线）
            drawLine(
                color = Color(proofLineColor),
                start = Offset(axisX, 0f),
                end = Offset(axisX, h),
                strokeWidth = 4f * density,
            )
        } else {
            drawLine(
                color = labelColorValue,
                start = Offset(axisX, 0f),
                end = Offset(axisX, h),
                strokeWidth = tickThickness,
            )
        }

        // ---- 6. Pointer — glowing dot at center（自证时替换为倒计时矩形） ----
        val ptrY = h / 2f
        if (proofActive) {
            // 光晕粒子：沿金线从下往上循环移动（多个粒子错开相位）
            val glowColor = Color(proofGlowColor)
            val baseRadius = 6f * density
            repeat(6) { i ->
                // p 从 0→1，y 从底部(h)→顶部(0)：从下往上
                val p = (glowProgress + i * 0.16f) % 1f
                val y = (1f - p) * h
                drawCircle(
                    color = glowColor.copy(alpha = 0.30f),
                    radius = baseRadius,
                    center = Offset(axisX, y),
                )
                // 拖尾小点（在下方）
                drawCircle(
                    color = glowColor.copy(alpha = 0.16f),
                    radius = baseRadius * 0.6f,
                    center = Offset(axisX, (y + 14f * density).coerceAtMost(h)),
                )
            }

            // 倒计时圆边矩形（替换红点）
            val rectW = 62f * density
            val rectH = 24f * density
            drawRoundRect(
                color = Color(proofCountdownBg),
                topLeft = Offset(axisX - rectW / 2f, ptrY - rectH / 2f),
                size = androidx.compose.ui.geometry.Size(rectW, rectH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rectH / 2f, rectH / 2f),
            )
            countdownPaint.apply {
                color = android.graphics.Color.argb(
                    255,
                    (Color(proofCountdownText).red * 255).toInt(),
                    (Color(proofCountdownText).green * 255).toInt(),
                    (Color(proofCountdownText).blue * 255).toInt(),
                )
                textSize = 14f * density
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                formatProofRemaining(remainingMs),
                axisX,
                ptrY + 14f * density * 0.35f,
                countdownPaint,
            )
        } else {
            // Glowing dot at axis intersection
            val dotCenter = Offset(axisX, ptrY)
            val dotColorValue = Color(dotColor)
            drawCircle(color = dotColorValue.copy(alpha = 0.12f), radius = 44f, center = dotCenter)
            drawCircle(color = dotColorValue.copy(alpha = 0.3f), radius = 24f, center = dotCenter)
            drawCircle(color = dotColorValue, radius = 8f, center = dotCenter)
        }

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

        // ---- 垂直 DstIn 蒙版：上下各 15% 虚化（smoothstep），中间保持不透明 ----
        drawRect(
            brush = Brush.verticalGradient(*VERTICAL_FADE_STOPS),
            blendMode = BlendMode.DstIn,
        )

        // 恢复图层：蒙版结果合成回画布
        drawContext.canvas.restore()
    }
}

/**
 * 预计算 exp + scale，避免每次调用 scoreToY 时重复 ln/pow（每帧 ~80 调用 → 1 次）。
 * 返回 Pair(exp, scale) 供 scoreToY 复用。
 */
private fun precomputeExpScale(
    halfH: Float,
    displayRange: Float,
    quarterValue: Float,
): Pair<Float, Float> {
    val exp = if (quarterValue > 0f && quarterValue < displayRange) {
        (ln(0.5) / ln((quarterValue / displayRange).toDouble())).toFloat()
    } else {
        1f
    }
    val scale = halfH / (displayRange.toDouble().pow(exp.toDouble())).toFloat()
    return Pair(exp, scale)
}

/** Non-linear score → Y mapping，复用预计算的 exp + scale。 */
private fun scoreToY(
    score: Float,
    centerScore: Float,
    canvasH: Float,
    displayRange: Float,
    exp: Float,
    scale: Float,
): Float {
    val d = score - centerScore
    val sign = sign(d)
    val absD = minOf(abs(d), displayRange * 2f)
    val pixelOffset = sign * (absD.toDouble().pow(exp.toDouble())).toFloat() * scale
    return canvasH / 2f - pixelOffset
}
