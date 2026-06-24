# 阶段 5：金线动画 Canvas — XiaoLiuRenThreadCanvas.kt

## 目标

创建 `XiaoLiuRenThreadCanvas.kt`，实现从大安出发、沿掐指路径游走的金色丝线动画。

核心技术：多层贝塞尔曲线 + 发光层 + 粒子尾迹 + 分阶段动画。

## 文件路径

```
app/src/main/java/com/example/karma/ui/divination/components/XiaoLiuRenThreadCanvas.kt
```

---

## 参考代码

动画驱动模式参考 [ParticleEngineCanvas.kt](../../app/src/main/java/com/example/karma/ui/prayer/ParticleEngineCanvas.kt)（已有成熟实现）。

---

## 详细设计

### 组件签名

```kotlin
@Composable
fun XiaoLiuRenThreadCanvas(
    fullPath: List<Int>,              // 完整推算路径（宫索引序列，如 [0,1,2,2,3,4,...]）
    palacePositions: List<PalacePosition>, // 六宫屏幕坐标
    animationPhase: AnimationPhase,    // 当前动画阶段
    onPhaseComplete: (AnimationPhase) -> Unit, // 阶段完成回调
    modifier: Modifier = Modifier,
)
```

### 动画时序常量

```kotlin
companion object {
    const val PHASE_MONTH_DURATION = 300   // 每步毫秒（月步较慢）
    const val PHASE_DAY_DURATION = 200     // 每步毫秒（日步稍快）
    const val PHASE_HOUR_DURATION = 250    // 每步毫秒（时步适中）
    const val PAUSE_DURATION = 500L        // 阶段间暂停
    const val GLOW_DURATION = 1000L        // 结果发光持续
}
```

### 路径段数据结构（内部）

```kotlin
private data class ThreadSegment(
    val fromIndex: Int,     // 起始宫索引
    val toIndex: Int,       // 目标宫索引
    val fromX: Float,       // 起始 X
    val fromY: Float,       // 起始 Y
    val toX: Float,         // 目标 X
    val toY: Float,         // 目标 Y
    val phase: AnimationPhase, // 所属阶段
)
```

### 动画帧循环

```kotlin
LaunchedEffect(fullPath, animationPhase) {
    if (animationPhase == AnimationPhase.IDLE || animationPhase == AnimationPhase.COMPLETE) return@LaunchedEffect

    val startTime = withFrameMillis { it }
    var phaseStart = startTime
    var currentPhase = animationPhase

    // 计算各阶段的总步数和时长
    // monthSteps = 从 fullPath 中提取月步部分
    // daySteps = 日步部分
    // hourSteps = 时步部分

    while (currentPhase != AnimationPhase.COMPLETE) {
        withFrameMillis { frameTime ->
            val elapsed = frameTime - phaseStart

            when (currentPhase) {
                AnimationPhase.COUNTING_MONTH -> {
                    val progress = (elapsed / (monthSteps * PHASE_MONTH_DURATION)).coerceAtMost(1f)
                    currentPathProgress = progress
                    if (progress >= 1f) {
                        phaseStart = frameTime  // 开始暂停计时
                        currentPhase = AnimationPhase.MONTH_PAUSE
                        onPhaseComplete(AnimationPhase.COUNTING_MONTH)
                    }
                }
                AnimationPhase.MONTH_PAUSE -> {
                    if (elapsed >= PAUSE_DURATION) {
                        phaseStart = frameTime
                        currentPhase = AnimationPhase.COUNTING_DAY
                    }
                }
                // ... DAY_PAUSE, COUNTING_HOUR 同理
                AnimationPhase.RESULT_GLOW -> {
                    val glowProgress = (elapsed / GLOW_DURATION.toFloat()).coerceAtMost(1f)
                    resultGlowProgress = glowProgress
                    if (glowProgress >= 1f) {
                        currentPhase = AnimationPhase.COMPLETE
                        onPhaseComplete(AnimationPhase.RESULT_GLOW)
                    }
                }
                else -> {}
            }
        }
    }
}
```

### 金线绘制算法

```kotlin
fun DrawScope.drawGoldenThread(progress: Float, segments: List<ThreadSegment>) {
    if (segments.isEmpty()) return

    val totalSegments = segments.size
    val exactPosition = progress * totalSegments
    val currentSegIdx = exactPosition.toInt().coerceAtMost(totalSegments - 1)
    val segProgress = exactPosition - currentSegIdx

    // 1. 绘制已完成的段（完整金线 + 残留辉光）
    for (i in 0 until currentSegIdx) {
        drawSegmentGlow(segments[i], completion = 1f)
        drawSegmentThreads(segments[i], completion = 1f)
    }

    // 2. 绘制当前段（部分进度）
    if (currentSegIdx < totalSegments) {
        val seg = segments[currentSegIdx]
        drawSegmentGlow(seg, completion = segProgress)
        drawSegmentThreads(seg, completion = segProgress)

        // 3. 在当前端点绘制粒子尾迹
        val currentEnd = lerp(Offset(seg.fromX, seg.fromY), Offset(seg.toX, seg.toY), segProgress)
        drawParticleTrail(currentEnd, segProgress)
    }
}

/**
 * 绘制单段的多层金线（4根重叠，控制点偏移产生飘逸感）
 */
fun DrawScope.drawSegmentThreads(seg: ThreadSegment, completion: Float) {
    val from = Offset(seg.fromX, seg.fromY)
    val to = Offset(seg.toX, seg.toY)
    val end = lerp(from, to, completion)
    val dx = to.x - from.x
    val dy = to.y - from.y

    repeat(4) { i ->
        // 每条线的控制点有不同偏移
        val cpOffset = (i - 1.5f) * 25.dp.toPx()  // -37.5, -12.5, +12.5, +37.5 dp
        val perpX = -dy / hypot(dx, dy) * cpOffset  // 垂直于段方向的偏移
        val perpY = dx / hypot(dx, dy) * cpOffset

        val cp1 = Offset(
            from.x + dx * 0.33f + perpX * 0.7f,
            from.y + dy * 0.33f + perpY * 0.7f,
        )
        val cp2 = Offset(
            from.x + dx * 0.66f - perpX * 0.7f,
            from.y + dy * 0.66f - perpY * 0.7f,
        )

        val path = Path().apply {
            moveTo(from.x, from.y)
            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y)
        }

        // 第3层：外发光（最宽，最透明）
        drawPath(path,
            color = Color(0xFFFFD700).copy(alpha = 0.12f),
            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))

        // 第2层：中发光
        drawPath(path,
            color = Color(0xFFFFD700).copy(alpha = 0.4f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

        // 第1层：主线（最细，最亮）
        drawPath(path,
            color = Color(0xFFFFD700).copy(alpha = 0.85f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}
```

### 粒子尾迹系统

```kotlin
private class TrailParticle(
    var x: Float, var y: Float,
    var alpha: Float,     // 0 → 1 → 0
    var radius: Float,    // 逐渐缩小
    var life: Float,      // 剩余生命 0-1
)

private val trailParticles = mutableListOf<TrailParticle>()
private const val MAX_PARTICLES = 50
private const val PARTICLE_SPAWN_INTERVAL = 0.05f // 每 5% 段进度的间隔

fun spawnParticle(x: Float, y: Float) {
    if (trailParticles.size >= MAX_PARTICLES) {
        trailParticles.removeAt(0) // FIFO 淘汰
    }
    trailParticles.add(TrailParticle(
        x = x + (random.nextFloat() - 0.5f) * 20.dp.toPx(),
        y = y + (random.nextFloat() - 0.5f) * 20.dp.toPx(),
        alpha = 0.8f + random.nextFloat() * 0.2f,
        radius = 2.dp.toPx() + random.nextFloat() * 3.dp.toPx(),
        life = 1f,
    ))
}

fun updateAndDrawParticles(drawScope: DrawScope, dt: Float) {
    val iterator = trailParticles.iterator()
    while (iterator.hasNext()) {
        val p = iterator.next()
        p.life -= dt * 0.8f  // 衰减速度
        p.alpha = p.life * 0.6f
        p.radius *= 0.98f

        if (p.life <= 0f) {
            iterator.remove()
        } else {
            drawScope.drawCircle(
                color = Color(0xFFFFD700).copy(alpha = p.alpha.coerceIn(0f, 1f)),
                radius = p.radius,
                center = Offset(p.x, p.y),
            )
        }
    }
}
```

### 结果发光效果（RESULT_GLOW 阶段）

```kotlin
if (animationPhase == AnimationPhase.RESULT_GLOW) {
    val resultPos = palacePositions[resultIndex]
    val pulseRadius = 30.dp.toPx() + sin(resultGlowProgress * PI * 3).toFloat() * 15.dp.toPx()
    val pulseAlpha = (0.3f + sin(resultGlowProgress * PI * 2).toFloat() * 0.3f).coerceIn(0f, 1f)

    // 脉冲光环
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = pulseAlpha),
        radius = pulseRadius,
        center = Offset(resultPos.centerX, resultPos.centerY),
    )
    // 第二层更大更淡的光环
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = pulseAlpha * 0.5f),
        radius = pulseRadius * 1.8f,
        center = Offset(resultPos.centerX, resultPos.centerY),
    )
}
```

---

## 阶段切换完整流程

```
用户点击"开始推算"
    ↓
ViewModel.startDivination()
    ├── 验证输入
    ├── calculate(month, day, hour) → fullPath + resultPalace
    └── animationPhase = COUNTING_MONTH

ThreadCanvas LaunchedEffect 检测到 animationPhase 变化
    ↓
COUNTING_MONTH (月步数完)
    → onPhaseComplete(COUNTING_MONTH) → ViewModel → MONTH_PAUSE
MONTH_PAUSE (暂停 500ms 后自动)
    → 直接切换到 COUNTING_DAY
COUNTING_DAY (日步数完)
    → onPhaseComplete(COUNTING_DAY) → ViewModel → DAY_PAUSE
DAY_PAUSE (暂停 500ms 后自动)
    → 直接切换到 COUNTING_HOUR
COUNTING_HOUR (时步数完)
    → onPhaseComplete(COUNTING_HOUR) → ViewModel → RESULT_GLOW
RESULT_GLOW (发光 1000ms 后)
    → onPhaseComplete(RESULT_GLOW) → ViewModel → COMPLETE
    → 显示 XiaoLiuRenResultPanel
```

---

## 验证检查点

- [ ] 金线从大安位置出发
- [ ] 金线按顺时针顺序在六宫之间正确游走
- [ ] 多层曲线有明显飘逸感
- [ ] 粒子尾迹平滑生成和消散
- [ ] 阶段间 500ms 暂停正常
- [ ] 结果发光脉冲动画流畅
- [ ] 动画总时长合理（约 4-7 秒，取决于步数）
- [ ] fullPath 为空时不崩溃

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
