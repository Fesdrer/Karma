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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.karma.ui.divination.AnimationPhase
import androidx.compose.runtime.withFrameNanos
import kotlin.math.PI
import kotlin.math.sin

// 不再使用 TrailPoint 数据类；拖尾直接以 (x: Float, y: Float) 对存储在 ArrayDeque 中，
// 避免每帧分配 TrailPoint 对象。

private class LightRing(
    val x: Float, val y: Float,
    var radius: Float,
    var alpha: Float,
    var life: Float,
)

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

    // palacePositions 在首次调用时已有 6 个元素且不再变化，仅以 fullPath 为 key 即可
    val segments = remember(fullPath) {
        buildSegments(fullPath, palacePositions, monthCount, dayCount)
    }

    val progress = remember { AnimProgress() }
    val trailBuffer = remember { ArrayDeque<Pair<Float, Float>>(40) }
    val rings = remember { mutableListOf<LightRing>() }

    // 光环尺寸（像素），在 Composable 中一次性转换
    val ringDp = with(LocalDensity.current) { 12.dp.toPx() to 80.dp.toPx() }
    val ringStartPx = ringDp.first
    val ringExpandPx = ringDp.second

    fun spawnRing(x: Float, y: Float) {
        rings.add(LightRing(x, y, ringStartPx, 0.7f, 1f))
    }

    // 动画阶段时长常量
    val monthStepMs = 250L
    val dayStepMs = 180L
    val hourStepMs = 200L
    val pauseMs = 500L
    val glowMs = 1000L

    // 缓存 segment 计数，避免每次 draw 时重复遍历
    val monthCount = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
    val dayCountCache = segments.count { it.phase == AnimationPhase.COUNTING_DAY }
    val hourCountCache = segments.count { it.phase == AnimationPhase.COUNTING_HOUR }

    LaunchedEffect(animationPhase) {
        progress.currentPhase = animationPhase
        progress.phaseProgress = 0f
        progress.resultGlowProgress = 0f
        trailBuffer.clear()

        var activePhase = animationPhase
        var finished = false

        val startNano = System.nanoTime()
        var phaseStartMs = 0L

        // 每个阶段独立的宫位追踪
        var phaseSegIdx = -1

        while (!finished) {
            withFrameNanos { frameNanos ->
            val elapsed = (frameNanos - startNano) / 1_000_000
            val phaseElapsed = elapsed - phaseStartMs

            when (activePhase) {
                AnimationPhase.COUNTING_MONTH -> {
                    val total = monthCount * monthStepMs
                    progress.phaseProgress = if (total > 0) (phaseElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    // 光环：当前走到第几个段（0..monthSteps）
                    val cur = (progress.phaseProgress * monthCount).toInt().coerceAtMost(monthCount)
                    if (cur != phaseSegIdx) {
                        if (cur < monthCount) {
                            val s = segments[cur]
                            spawnRing(s.fromX, s.fromY)
                        } else {
                            val s = segments[monthCount - 1]
                            spawnRing(s.toX, s.toY)
                        }
                        phaseSegIdx = cur
                    }
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
                        phaseSegIdx = -1
                        trailBuffer.clear()
                    }
                }
                AnimationPhase.COUNTING_DAY -> {
                    val offset = monthCount
                    val total = dayCountCache * dayStepMs
                    progress.phaseProgress = if (total > 0) (phaseElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    val cur = (progress.phaseProgress * dayCountCache).toInt().coerceAtMost(dayCountCache)
                    if (cur != phaseSegIdx) {
                        if (cur < dayCountCache) {
                            val s = segments[offset + cur]
                            spawnRing(s.fromX, s.fromY)
                        } else {
                            val s = segments[offset + dayCountCache - 1]
                            spawnRing(s.toX, s.toY)
                        }
                        phaseSegIdx = cur
                    }
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
                        phaseSegIdx = -1
                        trailBuffer.clear()
                    }
                }
                AnimationPhase.COUNTING_HOUR -> {
                    val offset = monthCount + dayCountCache
                    val total = hourCountCache * hourStepMs
                    progress.phaseProgress = if (total > 0) (phaseElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    val cur = (progress.phaseProgress * hourCountCache).toInt().coerceAtMost(hourCountCache)
                    if (cur != phaseSegIdx) {
                        if (cur < hourCountCache) {
                            val s = segments[offset + cur]
                            spawnRing(s.fromX, s.fromY)
                        } else {
                            val lastIdx = fullPath.last()
                            val p = palacePositions[lastIdx]
                            spawnRing(p.centerX, p.centerY)
                        }
                        phaseSegIdx = cur
                    }
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
            } // withFrameNanos
        } // while
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (segments.isEmpty()) return@Canvas

        // ---- 计算光点当前坐标 ----
        val overallProgress = when (progress.currentPhase) {
            AnimationPhase.COUNTING_MONTH, AnimationPhase.MONTH_PAUSE -> {
                val done = if (progress.currentPhase == AnimationPhase.MONTH_PAUSE) monthCount.toFloat()
                else monthCount * progress.phaseProgress
                if (monthCount > 0) done / segments.size.toFloat() else 0f
            }
            AnimationPhase.COUNTING_DAY, AnimationPhase.DAY_PAUSE -> {
                val done = if (progress.currentPhase == AnimationPhase.DAY_PAUSE) (monthCount + dayCountCache).toFloat()
                else monthCount + dayCountCache * progress.phaseProgress
                done / segments.size.toFloat()
            }
            AnimationPhase.COUNTING_HOUR, AnimationPhase.RESULT_GLOW -> {
                val done = if (progress.currentPhase == AnimationPhase.RESULT_GLOW) segments.size.toFloat()
                else monthCount + dayCountCache + hourCountCache * progress.phaseProgress
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

        val isGlowPhase = progress.currentPhase == AnimationPhase.RESULT_GLOW

        // ---- 更新拖尾缓冲区 ----
        trailBuffer.addLast(Pair(dotX, dotY))
        while (trailBuffer.size > 40) {
            trailBuffer.removeFirst()
        }

        // ---- 更新光环生命周期 ----
        updateRings(rings, ringStartPx, ringExpandPx)

        // ---- 绘制 ----
        drawRings(rings)
        drawTrail(trailBuffer, dotX, dotY)

        val pulseScale = if (isGlowPhase) {
            1f + sin(progress.resultGlowProgress * PI * 3).toFloat() * 0.4f
        } else {
            1f
        }
        drawLightDot(dotX, dotY, pulseScale)

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

// ====== 光环更新（Canvas 中调用） ======

private fun DrawScope.updateRings(rings: MutableList<LightRing>, startPx: Float, expandPx: Float) {
    val it = rings.iterator()
    while (it.hasNext()) {
        val ring = it.next()
        ring.life -= 0.022f
        ring.radius = startPx + (1f - ring.life) * expandPx
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
        drawCircle(
            ringColor.copy(alpha = ring.alpha * 0.25f),
            radius = ring.radius,
            center = Offset(ring.x, ring.y),
            style = Stroke(width = strokeWidth * 3f, cap = StrokeCap.Round),
        )
        drawCircle(
            ringColor.copy(alpha = ring.alpha),
            radius = ring.radius,
            center = Offset(ring.x, ring.y),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

// ====== 光点绘制 ======

private fun DrawScope.drawLightDot(x: Float, y: Float, pulseScale: Float) {
    val center = Offset(x, y)
    drawCircle(Color(0xFFFFD700).copy(alpha = 0.12f), radius = 32.dp.toPx() * pulseScale, center = center)
    drawCircle(Color(0xFFFFD700).copy(alpha = 0.4f), radius = 16.dp.toPx() * pulseScale, center = center)
    drawCircle(Color(0xFFFFF5E0).copy(alpha = 0.88f), radius = 6.dp.toPx() * pulseScale, center = center)
}

// ====== 拖尾绘制 ======

private fun DrawScope.drawTrail(buffer: ArrayDeque<Pair<Float, Float>>, dotX: Float, dotY: Float) {
    val size = buffer.size
    if (size < 2) return
    val baseWidth = 10.dp.toPx()
    val baseAlpha = 0.55f
    val totalCount = size + 1
    for (i in 0 until totalCount - 1) {
        val t = i.toFloat() / (totalCount - 1)
        val segWidth = baseWidth * t * t
        val segAlpha = baseAlpha * t * t
        if (segAlpha < 0.01f) continue
        val (x1, y1) = if (i < size) buffer[i] else Pair(dotX, dotY)
        val (x2, y2) = if (i + 1 < size) buffer[i + 1] else Pair(dotX, dotY)
        val p1 = Offset(x1, y1)
        val p2 = Offset(x2, y2)
        drawLine(Color(0xFFFFD700).copy(alpha = segAlpha * 0.25f), start = p1, end = p2, strokeWidth = segWidth * 3.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD700).copy(alpha = segAlpha), start = p1, end = p2, strokeWidth = segWidth, cap = StrokeCap.Round)
    }
}

// ====== 路径段构建 ======

private data class AnimSegment(
    val fromX: Float, val fromY: Float,
    val toX: Float, val toY: Float,
    val phase: AnimationPhase,
)

private fun buildSegments(
    path: List<Int>, positions: List<PalacePosition>,
    monthCount: Int, dayCount: Int,
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
