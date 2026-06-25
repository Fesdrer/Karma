package com.example.karma.ui.divination.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.karma.ui.divination.AnimationPhase
import kotlin.math.PI
import kotlin.math.sin

/** 拖尾轨迹点 */
private data class TrailPoint(val x: Float, val y: Float)

/** 扩散光环 */
private class LightRing(
    val x: Float,
    val y: Float,
    var radius: Float,
    var alpha: Float,
    var life: Float,
)

/** 用于 Canvas 内跨帧追踪的普通可变容器（不用 Compose state，避免触发重组） */
private class IntRef(var value: Int)
private class FloatRef(var value: Float)

/** 动画进度状态 */
private class AnimProgress(
    var phaseProgress: Float = 0f,
    var currentPhase: AnimationPhase = AnimationPhase.IDLE,
    var resultGlowProgress: Float = 0f,
    var animationTime: Long = 0L,
)

@Composable
fun XiaoLiuRenThreadCanvas(
    fullPath: List<Int>,
    palacePositions: List<PalacePosition>,
    animationPhase: AnimationPhase,
    monthCount: Int = 0,
    dayCount: Int = 0,
    hourCount: Int = 0,
    onPhaseComplete: (AnimationPhase) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (fullPath.isEmpty() || palacePositions.size < 6) return
    if (animationPhase == AnimationPhase.IDLE || animationPhase == AnimationPhase.COMPLETE) return

    val segments = remember(fullPath, palacePositions) {
        buildSegments(fullPath, palacePositions, monthCount, dayCount)
    }

    val progress = remember { AnimProgress() }
    val trailBuffer = remember { ArrayDeque<TrailPoint>(40) }
    // 普通 MutableList —— 在 Canvas draw lambda 中修改不会触发重组
    val rings = remember { mutableListOf<LightRing>() }

    // 跨帧追踪段索引 / 段内进度 —— 同样使用普通容器
    val lastSegIdx = remember { IntRef(-1) }
    val lastSegProgress = remember { FloatRef(0f) }
    val firstFrame = remember { IntRef(1) }

    // 动画阶段时长常量
    val monthStepMs = 250L
    val dayStepMs = 180L
    val hourStepMs = 200L
    val pauseMs = 500L
    val glowMs = 1000L

    val monthSteps = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
    val daySteps = segments.count { it.phase == AnimationPhase.COUNTING_DAY }
    val hourSteps = segments.count { it.phase == AnimationPhase.COUNTING_HOUR }

    LaunchedEffect(animationPhase) {
        progress.currentPhase = animationPhase
        progress.phaseProgress = 0f
        progress.resultGlowProgress = 0f
        trailBuffer.clear()
        rings.clear()
        firstFrame.value = 1

        var activePhase = animationPhase
        var finished = false

        kotlinx.coroutines.delay(16)
        val startNano = System.nanoTime()
        var phaseStartMs = 0L

        while (!finished) {
            kotlinx.coroutines.delay(16)
            val elapsed = (System.nanoTime() - startNano) / 1_000_000
            val phaseElapsed = elapsed - phaseStartMs

            when (activePhase) {
                AnimationPhase.COUNTING_MONTH -> {
                    val total = monthSteps * monthStepMs
                    progress.phaseProgress = if (total > 0) (phaseElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    if (phaseElapsed >= total) {
                        progress.phaseProgress = 1f
                        phaseStartMs = elapsed
                        activePhase = AnimationPhase.MONTH_PAUSE
                        onPhaseComplete(AnimationPhase.COUNTING_MONTH)
                    }
                }
                AnimationPhase.MONTH_PAUSE -> {
                    progress.phaseProgress = 1f
                    if (phaseElapsed >= pauseMs) {
                        phaseStartMs = elapsed
                        activePhase = AnimationPhase.COUNTING_DAY
                        trailBuffer.clear()
                    }
                }
                AnimationPhase.COUNTING_DAY -> {
                    val total = daySteps * dayStepMs
                    progress.phaseProgress = if (total > 0) (phaseElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    if (phaseElapsed >= total) {
                        progress.phaseProgress = 1f
                        phaseStartMs = elapsed
                        activePhase = AnimationPhase.DAY_PAUSE
                        onPhaseComplete(AnimationPhase.COUNTING_DAY)
                    }
                }
                AnimationPhase.DAY_PAUSE -> {
                    progress.phaseProgress = 1f
                    if (phaseElapsed >= pauseMs) {
                        phaseStartMs = elapsed
                        activePhase = AnimationPhase.COUNTING_HOUR
                        trailBuffer.clear()
                    }
                }
                AnimationPhase.COUNTING_HOUR -> {
                    val total = hourSteps * hourStepMs
                    progress.phaseProgress = if (total > 0) (phaseElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    if (phaseElapsed >= total) {
                        progress.phaseProgress = 1f
                        phaseStartMs = elapsed
                        activePhase = AnimationPhase.RESULT_GLOW
                        onPhaseComplete(AnimationPhase.COUNTING_HOUR)
                    }
                }
                AnimationPhase.RESULT_GLOW -> {
                    progress.phaseProgress = 1f
                    progress.resultGlowProgress = (phaseElapsed.toFloat() / glowMs).coerceIn(0f, 1f)
                    if (phaseElapsed >= glowMs) {
                        progress.resultGlowProgress = 1f
                        finished = true
                        onPhaseComplete(AnimationPhase.RESULT_GLOW)
                    }
                }
                else -> {}
            }

            progress.currentPhase = activePhase
            progress.animationTime = elapsed
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (segments.isEmpty()) return@Canvas

        // ---- 计算光点当前坐标 ----
        val overallProgress = when (progress.currentPhase) {
            AnimationPhase.COUNTING_MONTH, AnimationPhase.MONTH_PAUSE -> {
                val mCount = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
                val done = if (progress.currentPhase == AnimationPhase.MONTH_PAUSE) mCount.toFloat()
                else mCount * progress.phaseProgress
                if (mCount > 0) done / segments.size.toFloat() else 0f
            }
            AnimationPhase.COUNTING_DAY, AnimationPhase.DAY_PAUSE -> {
                val mCount = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
                val dCount = segments.count { it.phase == AnimationPhase.COUNTING_DAY }
                val done = if (progress.currentPhase == AnimationPhase.DAY_PAUSE) (mCount + dCount).toFloat()
                else mCount + dCount * progress.phaseProgress
                done / segments.size.toFloat()
            }
            AnimationPhase.COUNTING_HOUR, AnimationPhase.RESULT_GLOW -> {
                val mCount = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
                val dCount = segments.count { it.phase == AnimationPhase.COUNTING_DAY }
                val hCount = segments.count { it.phase == AnimationPhase.COUNTING_HOUR }
                val done = if (progress.currentPhase == AnimationPhase.RESULT_GLOW) segments.size.toFloat()
                else mCount + dCount + hCount * progress.phaseProgress
                done / segments.size.toFloat()
            }
            else -> 0f
        }.coerceIn(0f, 1f)

        val totalCount = segments.size
        val exactPos = overallProgress * totalCount
        val currentSegIdx = exactPos.toInt().coerceAtMost(totalCount - 1)
        val segProgress = exactPos - currentSegIdx

        val seg = segments[currentSegIdx]
        val from = Offset(seg.fromX, seg.fromY)
        val to = Offset(seg.toX, seg.toY)
        val dotX = from.x + (to.x - from.x) * segProgress.coerceIn(0f, 1f)
        val dotY = from.y + (to.y - from.y) * segProgress.coerceIn(0f, 1f)

        // ---- 检测光环发射时机 ----
        // 起始帧：大安放大光环
        if (firstFrame.value == 1 && currentSegIdx < segments.size) {
            spawnRing(rings, segments[0].fromX, segments[0].fromY)
            firstFrame.value = 0
        }

        // 段切换 = 到达新的 FROM 宫位 → 到达光环
        if (currentSegIdx != lastSegIdx.value && currentSegIdx < segments.size) {
            spawnRing(rings, segments[currentSegIdx].fromX, segments[currentSegIdx].fromY)
            lastSegIdx.value = currentSegIdx
        }

        // segProgress 跨过阈值 → 离开光环
        val departThreshold = 0.04f
        if (currentSegIdx == lastSegIdx.value &&
            lastSegProgress.value < departThreshold &&
            segProgress >= departThreshold
        ) {
            spawnRing(rings, seg.fromX, seg.fromY)
        }
        lastSegProgress.value = segProgress

        // ---- 更新拖尾缓冲区 ----
        trailBuffer.addLast(TrailPoint(dotX, dotY))
        while (trailBuffer.size > 40) {
            trailBuffer.removeFirst()
        }

        // ---- 更新光环生命周期 ----
        updateRings(rings)

        // ---- 绘制 ----
        // 1. 光环（最底层）
        drawRings(rings)

        // 2. 拖尾
        drawTrail(trailBuffer, dotX, dotY)

        // 3. 光点
        val isGlowPhase = progress.currentPhase == AnimationPhase.RESULT_GLOW
        val pulseScale = if (isGlowPhase) {
            1f + sin(progress.resultGlowProgress * PI * 3).toFloat() * 0.4f
        } else {
            1f
        }
        drawLightDot(dotX, dotY, pulseScale)

        // 4. 结果发光
        if (isGlowPhase && fullPath.isNotEmpty()) {
            val resultIdx = fullPath.last()
            if (resultIdx in palacePositions.indices) {
                val pos = palacePositions[resultIdx]
                val g = progress.resultGlowProgress
                val r = 22.dp.toPx() + sin(g * PI * 3).toFloat() * 10.dp.toPx()
                val a = (0.2f + sin(g * PI * 2).toFloat() * 0.2f).coerceIn(0f, 1f)
                drawCircle(Color(0xFFFFD700).copy(alpha = a), r, Offset(pos.centerX, pos.centerY))
                drawCircle(Color(0xFFFFD700).copy(alpha = a * 0.45f), r * 1.6f, Offset(pos.centerX, pos.centerY))
            }
        }
    }
}

// ====== 光环系统 ======

private fun DrawScope.spawnRing(rings: MutableList<LightRing>, x: Float, y: Float) {
    rings.add(LightRing(
        x = x, y = y,
        radius = 12.dp.toPx(),
        alpha = 0.7f,
        life = 1f,
    ))
}

private fun DrawScope.updateRings(rings: MutableList<LightRing>) {
    val it = rings.iterator()
    while (it.hasNext()) {
        val ring = it.next()
        ring.life -= 0.022f
        ring.radius = 12.dp.toPx() + (1f - ring.life) * 80.dp.toPx()
        ring.alpha = (ring.life * 0.7f).coerceIn(0f, 1f)
        if (ring.life <= 0f) {
            it.remove()
        }
    }
}

private fun DrawScope.drawRings(rings: List<LightRing>) {
    val ringColor = Color(0xFFFF4444)
    for (ring in rings) {
        if (ring.alpha < 0.01f) continue
        val strokeWidth = (3.5.dp.toPx() * ring.life).coerceAtLeast(0.5.dp.toPx())
        // 外发光层
        drawCircle(
            ringColor.copy(alpha = ring.alpha * 0.25f),
            radius = ring.radius,
            center = Offset(ring.x, ring.y),
            style = Stroke(width = strokeWidth * 3f, cap = StrokeCap.Round),
        )
        // 主环
        drawCircle(
            ringColor.copy(alpha = ring.alpha),
            radius = ring.radius,
            center = Offset(ring.x, ring.y),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

// ====== 光点绘制 ======

/** 绘制光点：三层叠加（外光晕 → 中辉光 → 内核亮点），直径×2 */
private fun DrawScope.drawLightDot(x: Float, y: Float, pulseScale: Float) {
    val center = Offset(x, y)

    // 外层光晕
    drawCircle(
        Color(0xFFFFD700).copy(alpha = 0.12f),
        radius = 32.dp.toPx() * pulseScale,
        center = center,
    )
    // 中层辉光
    drawCircle(
        Color(0xFFFFD700).copy(alpha = 0.4f),
        radius = 16.dp.toPx() * pulseScale,
        center = center,
    )
    // 内核亮点（偏白，最亮）
    drawCircle(
        Color(0xFFFFF5E0).copy(alpha = 0.88f),
        radius = 6.dp.toPx() * pulseScale,
        center = center,
    )
}

// ====== 拖尾绘制 ======

/** 绘制拖尾：从光点向尾部，宽度和透明度逐渐衰减，与光点同步放大 */
private fun DrawScope.drawTrail(
    buffer: ArrayDeque<TrailPoint>,
    dotX: Float,
    dotY: Float,
) {
    val size = buffer.size
    if (size < 2) return

    val points = buffer.toList() + TrailPoint(dotX, dotY)
    val n = points.size
    if (n < 2) return

    val baseWidth = 10.dp.toPx()
    val baseAlpha = 0.55f

    for (i in 0 until n - 1) {
        val t = i.toFloat() / (n - 1)
        val segWidth = baseWidth * t * t
        val segAlpha = baseAlpha * t * t

        if (segAlpha < 0.01f) continue

        val p1 = Offset(points[i].x, points[i].y)
        val p2 = Offset(points[i + 1].x, points[i + 1].y)

        // 外发光层
        drawLine(
            Color(0xFFFFD700).copy(alpha = segAlpha * 0.25f),
            start = p1, end = p2,
            strokeWidth = segWidth * 3.5f,
            cap = StrokeCap.Round,
        )
        // 主线
        drawLine(
            Color(0xFFFFD700).copy(alpha = segAlpha),
            start = p1, end = p2,
            strokeWidth = segWidth,
            cap = StrokeCap.Round,
        )
    }
}

// ====== 路径段构建 ======

private data class AnimSegment(
    val fromX: Float, val fromY: Float,
    val toX: Float, val toY: Float,
    val phase: AnimationPhase,
)

private fun buildSegments(
    path: List<Int>,
    positions: List<PalacePosition>,
    monthCount: Int,
    dayCount: Int,
): List<AnimSegment> {
    if (path.size < 2) return emptyList()
    val monthSeg = (monthCount - 1).coerceAtLeast(0)
    val daySeg = (dayCount - 1).coerceAtLeast(0)
    val segs = mutableListOf<AnimSegment>()
    for (i in 0 until path.size - 1) {
        val from = positions[path[i]]
        val to = positions[path[i + 1]]
        val phase = when {
            i < monthSeg -> AnimationPhase.COUNTING_MONTH
            i < monthSeg + daySeg -> AnimationPhase.COUNTING_DAY
            else -> AnimationPhase.COUNTING_HOUR
        }
        segs.add(AnimSegment(from.centerX, from.centerY, to.centerX, to.centerY, phase))
    }
    return segs
}
