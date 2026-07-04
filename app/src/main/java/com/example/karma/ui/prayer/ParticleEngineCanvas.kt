package com.example.karma.ui.prayer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class TierData(
    val threads: Int,
    val rings: Int,
    val sparks: Int,
    val duration: Long,
    val dark: Float,
    val glow: Float,
    val gold: Float,
)

private val TIERS = listOf(
    TierData(30, 2, 0, 3200L, 0.5f, 0f, 0f),
    TierData(50, 4, 25, 4200L, 0.6f, 8f, 0.12f),
    TierData(80, 6, 65, 5200L, 0.7f, 20f, 0.3f),
    TierData(150, 12, 160, 7000L, 0.88f, 45f, 0.55f),
)

private data class KarmaThread(
    val angle: Float,
    val finalLen: Float,
    val speed: Float,
    val cpOffset: Float,
    val cp2Offset: Float,
    val hue: Float,
    val sat: Float,
    val light: Float,
    val lineWidth: Float,
    val delay: Float,
    var currentLen: Float = 0f,
) {
    /** 可复用 native Path，避免每帧 new Path() 产生 GC 压力 */
    @kotlin.jvm.Transient
    val reusablePath: android.graphics.Path = android.graphics.Path()
}

private data class ExpandingRing(
    val finalRadius: Float,
    val speed: Float,
    val lineWidth: Float,
    val baseOpacity: Float,
    val delay: Float,
    var currentRadius: Float = 0f,
)

private data class Spark(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    val size: Float,
    val hue: Float, val sat: Float, val light: Float,
    var life: Float = 1f,
    val decay: Float,
)

private data class RuneParticle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    val char: String,
    val size: Float,
    var life: Float = 1f,
    val decay: Float,
    val hue: Float,
    var vyAccel: Float = 0.02f,
)

@Composable
fun ParticleEngineCanvas(
    amount: Float,
    purpose: String,
    rankLevel: Int,
    totalRanks: Int = 9,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1.5f,
) {
    val frameCount = remember { mutableIntStateOf(0) }
    val state = remember {
        ParticleState(
            tier = TIERS[when {
                rankLevel >= totalRanks -> 3
                rankLevel.toFloat() / totalRanks.toFloat() <= 0.33f -> 0
                rankLevel.toFloat() / totalRanks.toFloat() <= 0.66f -> 1
                else -> 2
            }],
            isDivine = rankLevel >= totalRanks,
            scale = scale,
        )
    }
    val runes = remember { mutableListOf<RuneParticle>() }

    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        val dur = state.tier.duration
        var isComplete = false

        while (!isComplete) {
            withFrameMillis { frameTime ->
                val elapsed = frameTime - startTime

                // Update particles
                state.update(elapsed, runes, frameTime - startTime < 2000L)

                // Trigger recomposition
                frameCount.intValue++

                if (elapsed > dur) {
                    isComplete = true
                    onComplete()
                    return@withFrameMillis
                }
            }
        }
    }

    // Pre-allocate Paint/TextPaint objects (created once, mutated per-frame)
    val runePaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val amountPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
    }
    val divineTextPaint = remember {
        android.text.TextPaint().apply { isFakeBoldText = true; isAntiAlias = true }
    }
    val divineBracketPaint = remember {
        android.graphics.Paint().apply { isFakeBoldText = true; isAntiAlias = true }
    }
    val normalTextPaint = remember {
        android.text.TextPaint().apply { isAntiAlias = true }
    }
    val normalBracketPaint = remember {
        android.graphics.Paint().apply { isAntiAlias = true }
    }
    // 复用 stroke paint 绘制金线（避免每帧每线程 new Path() + Compose Path→native 转换）
    val threadStrokePaint = remember {
        android.graphics.Paint().apply { style = android.graphics.Paint.Style.STROKE; isAntiAlias = true }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return@Canvas

        // Re-trigger on frame count change
        frameCount.intValue

        val cx = w / 2f
        val cy = h / 2f
        state.cx = cx
        state.cy = cy
        val baseScale = min(w, h)
        val diag = sqrt(w * w + h * h)
        val elapsed = state.elapsed
        val p = state.tier

        // ---- Divine central halo (tier 3) ----
        if (state.isDivine) {
            val haloR = baseScale * 0.08f + sin(elapsed / 800f) * baseScale * 0.03f
            drawCircle(
                color = Color(0xFFFFd700).copy(alpha = 0.35f),
                radius = haloR + 80f * scale,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = Color(0xFFFF6600).copy(alpha = 0.2f),
                radius = haloR + 60f * scale,
                center = Offset(cx, cy),
            )
        }

        // ---- Karmic threads ----
        for (t in state.threads) {
            if (t.currentLen <= 0f) continue
            val endX = cx + cos(t.angle.toDouble()).toFloat() * t.currentLen
            val endY = cy + sin(t.angle.toDouble()).toFloat() * t.currentLen
            val cp1x = cx + cos((t.angle + 0.6f).toDouble()).toFloat() * t.cpOffset
            val cp1y = cy + sin((t.angle + 0.6f).toDouble()).toFloat() * t.cpOffset
            val cp2x = cx + cos((t.angle - 0.4f).toDouble()).toFloat() * t.cp2Offset
            val cp2y = cy + sin((t.angle - 0.4f).toDouble()).toFloat() * t.cp2Offset
            val alpha = min(1f, t.currentLen / t.finalLen * 1.4f)

            val tp = t.reusablePath
            tp.reset()
            tp.moveTo(cx, cy)
            tp.cubicTo(cp1x, cp1y, cp2x, cp2y, endX, endY)
            val hslColor = Color.hsl(t.hue, t.sat / 100f, t.light / 100f, alpha)
            threadStrokePaint.apply {
                color = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    (hslColor.red * 255).toInt(),
                    (hslColor.green * 255).toInt(),
                    (hslColor.blue * 255).toInt(),
                )
                strokeWidth = t.lineWidth
            }
            drawContext.canvas.nativeCanvas.drawPath(tp, threadStrokePaint)
        }

        // ---- Expanding rings ----
        for (r in state.rings) {
            if (r.currentRadius <= 2f) continue
            val progress = r.currentRadius / r.finalRadius
            val ringAlpha = r.baseOpacity * maxOf(0f, 1f - progress * 1.1f)
            if (ringAlpha <= 0f) continue
            drawCircle(
                color = Color(0xFFff0000).copy(alpha = ringAlpha),
                radius = r.currentRadius,
                center = Offset(cx, cy),
                style = Stroke(width = maxOf(1f, r.lineWidth * (1f - progress * 0.7f))),
            )
        }

        // ---- Divine golden rings (tier 3) ----
        if (state.isDivine) {
            for (i in 0 until 4) {
                val phase = ((elapsed / 1000f) + i * 0.25f) % 1f
                val r = 40f * scale + phase * diag * 0.45f
                val ringAlpha = 0.4f * (1f - phase)
                if (ringAlpha <= 0f) continue
                drawCircle(
                    color = if (i % 2 == 0) Color(0xFFffd700) else Color(0xFFff6600),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = (3f - phase * 2f) * scale),
                    alpha = ringAlpha,
                )
            }
        }

        // ---- Sparks ----
        for (s in state.sparks) {
            if (s.life <= 0f) continue
            drawCircle(
                color = Color.hsl(s.hue, s.sat / 100f, s.light / 100f, maxOf(0f, s.life)),
                radius = maxOf(0.4f, s.size * s.life),
                center = Offset(s.x, s.y),
            )
        }

        // ---- Sacred rune particles (tier 3) ----
        if (state.isDivine) {
            for (r in runes) {
                if (r.life <= 0f) continue
                runePaint.apply {
                    color = android.graphics.Color.argb(
                        (maxOf(0f, r.life) * 0.7f * 255f).toInt(),
                        (55 + r.life * 20f).toInt().coerceIn(0, 255),
                        0, 0
                    )
                    textSize = r.size
                }
                drawContext.canvas.nativeCanvas.drawText(
                    r.char,
                    r.x,
                    r.y,
                    runePaint,
                )
            }
        }

        // ---- Text layer ----
        val dur = p.duration.toFloat()
        val textFade = min(elapsed / 500f, 1f)
        val textVisible = if (elapsed < dur - 500f) textFade
            else maxOf(0f, 1f - (elapsed - (dur - 500f)) / 500f)
        if (textVisible > 0f) {
            val textAlpha = (textVisible * 255f).toInt().coerceIn(0, 255)
            // 确保 StaticLayout 已缓存（仅在 canvas 宽度或 purpose 变化时重建）
            state.ensureLayouts(w, purpose, divineTextPaint, normalTextPaint, scale)
            if (state.isDivine) {
                amountPaint.apply {
                    color = android.graphics.Color.argb(textAlpha, 255, 68, 68)
                    textSize = 52f * scale
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "扣除 $amount 分",
                    cx,
                    cy + 5f * scale,
                    amountPaint,
                )
                // 使用缓存的 StaticLayout 绘制 purpose 文本
                divineBracketPaint.apply {
                    color = android.graphics.Color.argb(textAlpha, 255, 34, 34)
                    textSize = 44f * scale
                }
                val cachedLayout = state.cachedDivineLayout
                if (cachedLayout != null) {
                    val divineMaxWidth = (w * 0.74f).toInt()
                    drawContext.canvas.save()
                    val layoutX = cx - divineMaxWidth / 2f
                    val layoutY = cy + 55f * scale
                    drawContext.canvas.translate(layoutX, layoutY)
                    cachedLayout.draw(drawContext.canvas.nativeCanvas)
                    //「左上角
                    val bracketPad = 8f * scale
                    drawContext.canvas.nativeCanvas.drawText(
                        "「", bracketPad, divineTextPaint.textSize, divineBracketPaint
                    )
                    //」右下角
                    val lastLineBot = cachedLayout.height.toFloat()
                    val bracketW = divineBracketPaint.measureText("」")
                    drawContext.canvas.nativeCanvas.drawText(
                        "」",
                        divineMaxWidth - bracketW - bracketPad,
                        lastLineBot,
                        divineBracketPaint
                    )
                    drawContext.canvas.restore()
                }
            } else {
                amountPaint.apply {
                    color = android.graphics.Color.argb(textAlpha, 255, 34, 34)
                    textSize = 40f * scale
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "扣除 $amount 分",
                    cx,
                    cy,
                    amountPaint,
                )
                // 使用缓存的 StaticLayout 绘制 purpose 文本
                normalBracketPaint.apply {
                    color = android.graphics.Color.argb(textAlpha, 255, 0, 0)
                    textSize = 36f * scale
                }
                val cachedLayout = state.cachedNormalLayout
                if (cachedLayout != null) {
                    val normalMaxWidth = (w * 0.74f).toInt()
                    drawContext.canvas.save()
                    val nLayoutX = cx - normalMaxWidth / 2f
                    val nLayoutY = cy + 45f * scale
                    drawContext.canvas.translate(nLayoutX, nLayoutY)
                    cachedLayout.draw(drawContext.canvas.nativeCanvas)
                    //「左上角
                    val nBracketPad = 6f * scale
                    drawContext.canvas.nativeCanvas.drawText(
                        "「", nBracketPad, normalTextPaint.textSize, normalBracketPaint
                    )
                    //」右下角
                    val nLastLineBot = cachedLayout.height.toFloat()
                    val nBracketW = normalBracketPaint.measureText("」")
                    drawContext.canvas.nativeCanvas.drawText(
                        "」",
                        normalMaxWidth - nBracketW - nBracketPad,
                        nLastLineBot,
                        normalBracketPaint
                    )
                    drawContext.canvas.restore()
                }
            }
        }
    }
}

private class ParticleState(
    val tier: TierData,
    val isDivine: Boolean,
    val scale: Float = 1.5f,
) {
    var elapsed: Float = 0f
    val threads = mutableListOf<KarmaThread>()
    val rings = mutableListOf<ExpandingRing>()
    val sparks = mutableListOf<Spark>()
    private var initialized = false
    var cx = 500f
    var cy = 500f

    // 缓存的 StaticLayout，避免每帧 rebuild（文字布局是昂贵操作）
    var cachedDivineLayout: android.text.StaticLayout? = null
    var cachedNormalLayout: android.text.StaticLayout? = null
    private var lastLayoutCanvasW: Float = -1f
    private var lastLayoutPurpose: String = ""

    /** 在 Canvas 中调用：仅在 canvas 宽度或 purpose 变化时重建 StaticLayout */
    fun ensureLayouts(
        canvasW: Float,
        purpose: String,
        divineTextPaint: android.text.TextPaint,
        normalTextPaint: android.text.TextPaint,
        scale: Float,
    ) {
        if (canvasW == lastLayoutCanvasW && purpose == lastLayoutPurpose) return
        lastLayoutCanvasW = canvasW
        lastLayoutPurpose = purpose
        val maxWidth = (canvasW * 0.74f).toInt()
        if (isDivine && maxWidth > 0) {
            divineTextPaint.textSize = 44f * scale
            cachedDivineLayout = android.text.StaticLayout.Builder
                .obtain(purpose, 0, purpose.length, divineTextPaint, maxWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .build()
        } else if (!isDivine && maxWidth > 0) {
            normalTextPaint.textSize = 36f * scale
            cachedNormalLayout = android.text.StaticLayout.Builder
                .obtain(purpose, 0, purpose.length, normalTextPaint, maxWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
                .build()
        }
    }

    fun update(elapsedMs: Long, runes: MutableList<RuneParticle>, canSpawnRunes: Boolean) {
        elapsed = elapsedMs.toFloat()

        if (!initialized) {
            initialized = true
            initializeParticles()
        }

        // Update threads
        for (t in threads) {
            if (elapsedMs > t.delay.toLong()) {
                t.currentLen = min(t.finalLen, t.currentLen + t.speed)
            }
        }

        // Update rings
        for (r in rings) {
            if (elapsedMs > r.delay.toLong()) {
                r.currentRadius = min(r.finalRadius, r.currentRadius + r.speed)
            }
        }

        // Update sparks
        for (s in sparks) {
            s.x += s.vx; s.y += s.vy
            s.life -= s.decay; s.vx *= 0.99f; s.vy *= 0.99f
        }
        val sparkIter = sparks.iterator()
        while (sparkIter.hasNext()) {
            if (sparkIter.next().life <= 0f) sparkIter.remove()
        }

        // Update runes (tier 3)
        if (isDivine && canSpawnRunes && Random.nextFloat() < 0.15f) {
            val runeChars = listOf("卍", "✦", "⚘", "✧", "◇", "☯")
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val dist = (60f + Random.nextFloat() * 120f) * scale
            runes.add(
                RuneParticle(
                    x = cx + cos(a.toDouble()).toFloat() * dist,
                    y = cy + sin(a.toDouble()).toFloat() * dist,
                    vx = cos(a.toDouble()).toFloat() * (1f + Random.nextFloat() * 2f),
                    vy = sin(a.toDouble()).toFloat() * (1f + Random.nextFloat() * 2f) - 1.5f,
                    char = runeChars[Random.nextInt(runeChars.size)],
                    size = (14f + Random.nextFloat() * 20f) * scale,
                    decay = 0.003f + Random.nextFloat() * 0.005f,
                    hue = if (Random.nextFloat() < 0.4f) 42f else Random.nextFloat() * 15f,
                )
            )
        }
        for (r in runes) {
            r.x += r.vx; r.y += r.vy
            r.life -= r.decay; r.vy += 0.02f
        }
        val runeIter = runes.iterator()
        while (runeIter.hasNext()) {
            if (runeIter.next().life <= 0f) runeIter.remove()
        }
    }

    private fun initializeParticles() {
        val baseScale = 800f * scale

        for (i in 0 until tier.threads) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val finalLen = 80f + Random.nextFloat() * (baseScale * 0.45f)
            val hasGold = Random.nextFloat() < tier.gold
            threads.add(
                KarmaThread(
                    angle = angle,
                    finalLen = finalLen,
                    speed = 0.5f + Random.nextFloat() * 0.7f,
                    cpOffset = (Random.nextFloat() - 0.5f) * finalLen * 0.7f,
                    cp2Offset = (Random.nextFloat() - 0.5f) * finalLen * 0.5f,
                    hue = if (hasGold) 38f else Random.nextFloat() * 12f,
                    sat = if (hasGold) 90f else 92f + Random.nextFloat() * 8f,
                    light = if (hasGold) 55f + Random.nextFloat() * 20f else 30f + Random.nextFloat() * 25f,
                    lineWidth = (1f + Random.nextFloat() * 2.5f) * scale,
                    delay = Random.nextFloat() * 400f,
                )
            )
        }

        val maxRingR = baseScale * 0.42f
        for (i in 0 until tier.rings) {
            rings.add(
                ExpandingRing(
                    finalRadius = (80f + Random.nextFloat() * maxRingR) * scale,
                    speed = (0.8f + Random.nextFloat() * 1.8f) * scale,
                    lineWidth = (2f + Random.nextFloat() * 4f) * scale,
                    baseOpacity = 0.4f + Random.nextFloat() * 0.35f,
                    delay = i * (250f + Random.nextFloat() * 300f),
                )
            )
        }

        for (i in 0 until tier.sparks) {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val sp = (1.5f + Random.nextFloat() * 4.5f) * scale
            val isGold = Random.nextFloat() < tier.gold * 1.5f
            sparks.add(
                Spark(
                    x = cx, y = cy,
                    vx = cos(a.toDouble()).toFloat() * sp,
                    vy = sin(a.toDouble()).toFloat() * sp,
                    size = (1.2f + Random.nextFloat() * 3f) * scale,
                    hue = if (isGold) 42f else Random.nextFloat() * 10f,
                    sat = if (isGold) 95f else 100f,
                    light = if (isGold) 65f else 40f + Random.nextFloat() * 25f,
                    decay = 0.004f + Random.nextFloat() * 0.012f,
                )
            )
        }
    }
}
