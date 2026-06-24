package com.example.karma.ui.divination.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.karma.ui.divination.AnimationPhase
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/** 尾迹粒子 */
private class TrailParticle(
    var x: Float, var y: Float,
    var alpha: Float,
    var radius: Float,
    var life: Float,
)

/** 动画进度状态 */
private class AnimProgress(
    var phaseProgress: Float = 0f,     // 当前阶段内进度 0-1
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
    val particles = remember { mutableListOf<TrailParticle>() }
    val random = remember { Random(42) }

    // 动画阶段时长常量
    val monthStepMs = 250L
    val dayStepMs = 180L
    val hourStepMs = 200L
    val pauseMs = 500L
    val glowMs = 1000L

    // 各阶段步数
    val monthSteps = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
    val daySteps = segments.count { it.phase == AnimationPhase.COUNTING_DAY }
    val hourSteps = segments.count { it.phase == AnimationPhase.COUNTING_HOUR }

    LaunchedEffect(animationPhase) {
        progress.currentPhase = animationPhase
        progress.phaseProgress = 0f
        progress.resultGlowProgress = 0f

        var phaseStartMs = 0L
        var activePhase = animationPhase
        var finished = false

        // 获取初始帧时间
        kotlinx.coroutines.delay(16)
        val startNano = System.nanoTime()
        phaseStartMs = 0L

        while (!finished) {
            kotlinx.coroutines.delay(16)
            val elapsed = (System.nanoTime() - startNano) / 1_000_000

            when (activePhase) {
                AnimationPhase.COUNTING_MONTH -> {
                    val total = monthSteps * monthStepMs
                    progress.phaseProgress = if (total > 0) (elapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    if (elapsed >= total) {
                        progress.phaseProgress = 1f
                        startNano.let { phaseStartMs = elapsed }
                        activePhase = AnimationPhase.MONTH_PAUSE
                        onPhaseComplete(AnimationPhase.COUNTING_MONTH)
                    }
                }
                AnimationPhase.MONTH_PAUSE -> {
                    val pauseElapsed = elapsed - (monthSteps * monthStepMs)
                    progress.phaseProgress = 1f
                    if (pauseElapsed >= pauseMs) {
                        activePhase = AnimationPhase.COUNTING_DAY
                        // 重置基准时间
                    }
                }
                AnimationPhase.COUNTING_DAY -> {
                    val dayStart = monthSteps * monthStepMs + pauseMs
                    val dayElapsed = elapsed - dayStart
                    val total = daySteps * dayStepMs
                    progress.phaseProgress = if (total > 0) (dayElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    if (dayElapsed >= total) {
                        progress.phaseProgress = 1f
                        activePhase = AnimationPhase.DAY_PAUSE
                        onPhaseComplete(AnimationPhase.COUNTING_DAY)
                    }
                }
                AnimationPhase.DAY_PAUSE -> {
                    val dayEnd = monthSteps * monthStepMs + pauseMs + daySteps * dayStepMs
                    val pauseElapsed = elapsed - dayEnd
                    progress.phaseProgress = 1f
                    if (pauseElapsed >= pauseMs) {
                        activePhase = AnimationPhase.COUNTING_HOUR
                    }
                }
                AnimationPhase.COUNTING_HOUR -> {
                    val hourStart = monthSteps * monthStepMs + pauseMs + daySteps * dayStepMs + pauseMs
                    val hourElapsed = elapsed - hourStart
                    val total = hourSteps * hourStepMs
                    progress.phaseProgress = if (total > 0) (hourElapsed.toFloat() / total).coerceIn(0f, 1f) else 1f
                    if (hourElapsed >= total) {
                        progress.phaseProgress = 1f
                        activePhase = AnimationPhase.RESULT_GLOW
                        onPhaseComplete(AnimationPhase.COUNTING_HOUR)
                    }
                }
                AnimationPhase.RESULT_GLOW -> {
                    val glowStart = monthSteps * monthStepMs + pauseMs + daySteps * dayStepMs + pauseMs + hourSteps * hourStepMs
                    val glowElapsed = elapsed - glowStart
                    progress.phaseProgress = 1f
                    progress.resultGlowProgress = (glowElapsed.toFloat() / glowMs).coerceIn(0f, 1f)
                    if (glowElapsed >= glowMs) {
                        progress.resultGlowProgress = 1f
                        finished = true
                        onPhaseComplete(AnimationPhase.RESULT_GLOW)
                    }
                }
                else -> {}
            }

            progress.animationTime = elapsed
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (segments.isEmpty()) return@Canvas

        // 绘制所有已完成的段 + 当前进度段
        val overallProgress = when (progress.currentPhase) {
            AnimationPhase.COUNTING_MONTH, AnimationPhase.MONTH_PAUSE -> {
                val mCount = segments.count { it.phase == AnimationPhase.COUNTING_MONTH }
                val done = if (progress.currentPhase == AnimationPhase.MONTH_PAUSE) mCount.toFloat()
                else mCount * progress.phaseProgress
                done / segments.size.toFloat()
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

        // 已完成段
        for (i in 0 until currentSegIdx) {
            if (i < segments.size) {
                drawSegmentThreads(segments[i], 1f)
            }
        }

        // 当前段
        if (currentSegIdx < segments.size) {
            val seg = segments[currentSegIdx]
            drawSegmentThreads(seg, segProgress.coerceIn(0f, 1f))

            // 粒子
            val from = Offset(seg.fromX, seg.fromY)
            val to = Offset(seg.toX, seg.toY)
            val end = lerp(from, to, segProgress.coerceIn(0f, 1f))
            if (random.nextFloat() < 0.35f) {
                spawnParticle(end.x, end.y, particles, random)
            }
        }

        // 粒子更新绘制
        updateParticles(particles)

        // 结果发光
        if (progress.currentPhase == AnimationPhase.RESULT_GLOW && fullPath.isNotEmpty()) {
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

/** 构建路径段（按实际步数分配阶段标记） */
private fun buildSegments(
    path: List<Int>,
    positions: List<PalacePosition>,
    monthCount: Int,
    dayCount: Int,
): List<AnimSegment> {
    if (path.size < 2) return emptyList()
    // 各阶段的动画段数 = 步数 - 1（原地不动则为 0）
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

private data class AnimSegment(
    val fromX: Float, val fromY: Float,
    val toX: Float, val toY: Float,
    val phase: AnimationPhase,
)

/** 绘制单段的 4 层金线 */
private fun DrawScope.drawSegmentThreads(seg: AnimSegment, completion: Float) {
    val from = Offset(seg.fromX, seg.fromY)
    val to = Offset(seg.toX, seg.toY)
    val end = lerp(from, to, completion)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val segLen = hypot(dx, dy)
    if (segLen < 1f) return

    val threadCount = 4
    repeat(threadCount) { i ->
        val cpOffset = (i - (threadCount - 1) / 2f) * 20.dp.toPx()
        val perpX = -dy / segLen * cpOffset
        val perpY = dx / segLen * cpOffset

        val cp1 = Offset(from.x + dx * 0.33f + perpX * 0.7f, from.y + dy * 0.33f + perpY * 0.7f)
        val cp2 = Offset(from.x + dx * 0.66f - perpX * 0.7f, from.y + dy * 0.66f - perpY * 0.7f)

        val path = Path().apply {
            moveTo(from.x, from.y)
            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
        }

        // 外发光层
        drawPath(path, Color(0xFFFFD700).copy(alpha = 0.1f),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
        // 中发光层
        drawPath(path, Color(0xFFFFD700).copy(alpha = 0.32f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        // 主线
        drawPath(path, Color(0xFFFFD700).copy(alpha = 0.78f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
    }
}

private fun DrawScope.spawnParticle(x: Float, y: Float, particles: MutableList<TrailParticle>, random: Random) {
    if (particles.size >= 45) particles.removeAt(0)
    particles.add(TrailParticle(
        x = x + (random.nextFloat() - 0.5f) * 14.dp.toPx(),
        y = y + (random.nextFloat() - 0.5f) * 14.dp.toPx(),
        alpha = 0.65f + random.nextFloat() * 0.35f,
        radius = 1.2.dp.toPx() + random.nextFloat() * 2.dp.toPx(),
        life = 1f,
    ))
}

private fun DrawScope.updateParticles(particles: MutableList<TrailParticle>) {
    val it = particles.iterator()
    while (it.hasNext()) {
        val p = it.next()
        p.life -= 0.028f
        p.alpha = p.life * 0.45f
        p.radius *= 0.986f
        if (p.life <= 0f) it.remove()
        else drawCircle(
            Color(0xFFFFD700).copy(alpha = p.alpha.coerceIn(0f, 1f)),
            p.radius, Offset(p.x, p.y),
        )
    }
}

private fun lerp(from: Offset, to: Offset, t: Float): Offset =
    Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
