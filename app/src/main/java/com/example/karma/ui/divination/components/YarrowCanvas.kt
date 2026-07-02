package com.example.karma.ui.divination.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.divination.YarrowPhase
import com.example.karma.ui.divination.YarrowUiState
import kotlin.math.min

@Composable
fun YarrowCanvas(
    state: YarrowUiState, onUserTap: (Int) -> Unit, onPhaseComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val D = LocalDensity.current; val TM = rememberTextMeasurer()
    val prog = remember(state.phase) { Animatable(0f) }
    val sw = with(D) { 3.dp.toPx() }; val sg = with(D) { 1.8.dp.toPx() }
    val step = sw + sg; val sh = with(D) { 90.dp.toPx() }
    val tjiY = with(D) { 60.dp.toPx() }; val tjiHalf = sh / 2f
    val wTop = with(D) { 180.dp.toPx() }; val wBot = wTop + sh; val wCY = (wTop + wBot) / 2f
    val hangY = with(D) { 400.dp.toPx() }; val colY = with(D) { 520.dp.toPx() }
    val lnB = with(D) { 570.dp.toPx() }; val lnH = with(D) { 16.dp.toPx() }
    val mg = with(D) { 6.dp.toPx() }; val lx = mg

    LaunchedEffect(state.phase) {
        val dur = when (state.phase) {
            YarrowPhase.INTRO -> 1200; YarrowPhase.SPLITTING -> 500; YarrowPhase.HANGING_ONE -> 400
            YarrowPhase.GROUP_LEFT -> 400; YarrowPhase.COLLECT_LEFT -> 400
            YarrowPhase.GROUP_RIGHT -> 400; YarrowPhase.COLLECT_RIGHT -> 400
            YarrowPhase.STORING -> 600; YarrowPhase.FLASH_GROUPS -> 800
            YarrowPhase.MERGING -> 600; YarrowPhase.LINE_END -> 800
            else -> 0
        }
        if (dur > 0) { prog.animateTo(1f, tween(dur)); onPhaseComplete() }
    }

    Canvas(modifier = modifier.fillMaxSize().pointerInput(state.phase, state.n, step, sw, sg) {
        if (state.phase == YarrowPhase.WAITING) detectTapGestures { onUserTap(0) }
        if (state.phase == YarrowPhase.IDLE) detectTapGestures { tap ->
            val cw = size.width.toFloat(); val cx2 = cw / 2f; val n2 = state.n
            val wd2 = n2 * step - sg; val sx = cx2 - wd2 / 2f
            val l = sx + sw / 2f; val r = sx + wd2 - sw / 2f
            if (r > l) { val rt = ((tap.x - l) / (r - l)).coerceIn(0f, 1f); onUserTap(minOf(n2 - 2, maxOf(1, (rt * n2).toInt()))) }
        }
    }) {
        // 不画不透明背景——让 Theme.kt 的「地黄」暖光透出，匹配设置页面的金色过渡
        val cw = size.width; val cx = cw / 2f; val p = prog.value
        val n = state.n; val num = state.num; val ln = state.ln; val rn = state.rn
        val collected = ln + 1 + rn

        fun wd(k: Int) = k * step - sg
        fun stX(k: Int) = cx - wd(k) / 2f
        fun row(k: Int, sx: Float) = (0 until k).map { sx + it * step + sw / 2f }
        fun vs(x: Float, top: Float, bot: Float, a: Float = 1f) {
            drawLine(Color(0xFF00FF00).copy(alpha = a), Offset(x, top), Offset(x, bot), sw)
        }
        fun vh(x: Float, cy: Float, h: Float, a: Float = 1f) {
            drawLine(Color(0xFF00FF00).copy(alpha = a), Offset(x, cy - h / 2f), Offset(x, cy + h / 2f), sw)
        }
        // g4(k, refX, false) = 从左到右4根一组，余数在最右
        // g4(k, refX, true)  = 从右到左4根一组，余数在最左
        fun g4(k: Int, refX: Float, rtl: Boolean): List<Float> {
            if (k <= 0) return emptyList(); val xs = FloatArray(k)
            if (rtl) { var i = k - 1; var px = refX - sw / 2f; while (i >= 0) { val g = min(4, i + 1); for (j in 0 until g) xs[i - j] = px - j * step; i -= g; px -= g * step + step * 1.5f } }
            else { var i = 0; var px = refX + sw / 2f; while (i < k) { val g = min(4, k - i); for (j in 0 until g) xs[i + j] = px + j * step; i += g; px += g * step + step * 1.5f } }
            return xs.toList()
        }

        // === 太极 ===
        if (state.phase != YarrowPhase.INTRO && state.phase != YarrowPhase.WAITING) {
            drawLine(Color(0xFF00FF00), Offset(cx - tjiHalf, tjiY), Offset(cx + tjiHalf, tjiY), 3.dp.toPx())
            drawText(TM.measure("太极", style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 10.sp, color = Color(0xFF555577))),
                topLeft = Offset(cx - 12.dp.toPx(), tjiY - 18.dp.toPx()))
        }
        // === b 区历史 ===
        if (state.bSize > 0 && state.phase != YarrowPhase.LINE_END && state.phase != YarrowPhase.COMPLETE && state.phase != YarrowPhase.REVELATION_READY) {
            row(state.bSize, lx + sw / 2f).forEach { vs(it, colY - sh / 2f, colY + sh / 2f, 0.35f) }
        }

        val rEnd = cw - mg

        when (state.phase) {

            // ── WAITING：开始按钮 ──
            YarrowPhase.WAITING -> {
                val bw = 260.dp.toPx(); val bh = 120.dp.toPx()
                val bx = cx - bw / 2f; val by = wCY - bh / 2f
                // 外框
                drawRoundRect(Color(0xFFffd700).copy(alpha = 0.8f), Offset(bx, by), Size(bw, bh),
                    CornerRadius(16.dp.toPx()), style = Stroke(2.dp.toPx()))
                drawRoundRect(Color(0xFF0A0A0A), Offset(bx + 3, by + 3), Size(bw - 6, bh - 6),
                    CornerRadius(14.dp.toPx()))
                // 文字
                val title = "大衍之数五十"
                val sub = "其用四十有九"
                val btn = "☰  开始占筮  ☰"
                val ts = TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, color = Color(0xFFdaa520))
                val ss = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, color = Color(0xFFaa8844))
                val bs = TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, color = Color(0xFFffd700))
                val tw = TM.measure(title, style = ts).size.width.toFloat()
                val sw2 = TM.measure(sub, style = ss).size.width.toFloat()
                val bw2 = TM.measure(btn, style = bs).size.width.toFloat()
                drawText(TM.measure(title, style = ts), topLeft = Offset(cx - tw / 2f, by + 14.dp.toPx()))
                drawText(TM.measure(sub, style = ss), topLeft = Offset(cx - sw2 / 2f, by + 44.dp.toPx()))
                drawText(TM.measure(btn, style = bs), topLeft = Offset(cx - bw2 / 2f, by + 72.dp.toPx()))
            }

            // ── INTRO：a[0]一边旋转90度一边移动到最上方 ──
            YarrowPhase.INTRO -> {
                val xs50 = row(50, stX(50)); val ch = 25
                val p1 = (p / 0.55f).coerceIn(0f, 1f); val p2 = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
                for (i in 0 until 50) {
                    if (i == ch) drawLine(Color(0xFF00FF00), Offset(lerp(xs50[i], cx - tjiHalf, p1), lerp(wTop, tjiY, p1)),
                        Offset(lerp(xs50[i], cx + tjiHalf, p1), lerp(wBot, tjiY, p1)), sw)
                    else vs(lerp(xs50[i], row(49, stX(49))[if (i < ch) i else i - 1], p2), wTop, wBot)
                }
                if (p > 0.4f) drawText(TM.measure("太极", style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 10.sp, color = Color(0xFF555577).copy(alpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)))),
                    topLeft = Offset(cx - 12.dp.toPx(), tjiY - 18.dp.toPx()))
            }

            // ── IDLE ──
            YarrowPhase.IDLE -> {
                row(n, stX(n)).forEach { vs(it, wTop, wBot) }
                val isFirst = state.lines.isEmpty() && state.changeNumber == 1
                val title = if (isFirst) "大衍之数五十，其用四十有九" else "第${state.changeNumber}变 · 第${state.lines.size + 1}爻"
                val sub = if (isFirst) "分而为二以象两 · 点击分策" else "点击屏幕分策"
                // 按钮边框
                val bw = 240.dp.toPx(); val bh = 56.dp.toPx()
                val bx = cx - bw / 2f; val by = wTop - 80.dp.toPx()
                drawRoundRect(Color(0xFFb8860b).copy(alpha = 0.6f), Offset(bx, by), Size(bw, bh),
                    CornerRadius(12.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                drawRoundRect(Color(0xFFb8860b).copy(alpha = 0.12f), Offset(bx + 2, by + 2), Size(bw - 4, bh - 4),
                    CornerRadius(10.dp.toPx()))
                drawText(TM.measure(title, style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp, color = Color(0xFFdaa520))),
                    topLeft = Offset(cx - TM.measure(title, style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 15.sp)).size.width / 2f, by + 8.dp.toPx()))
                drawText(TM.measure(sub, style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 11.sp, color = Color(0xFF888888))),
                    topLeft = Offset(cx - TM.measure(sub, style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 11.sp)).size.width / 2f, by + 30.dp.toPx()))
            }

            // ── SPLITTING：a[1..num]左移, a[num+1..n]右移 ──
            YarrowPhase.SPLITTING -> {
                val from = row(n, stX(n))
                val leftTo = row(num, lx)
                val rCnt = n - num; val rightTo = row(rCnt, rEnd - wd(rCnt))
                for (i in 0 until num) vs(lerp(from[i], leftTo[i], p), wTop, wBot)
                for (i in 0 until rCnt) vs(lerp(from[num + i], rightTo[i], p), wTop, wBot)
            }

            // ── HANGING_ONE：a[num+1]缩短下移 ──
            YarrowPhase.HANGING_ONE -> {
                val leftTo = row(num, lx); val rCnt = n - num
                val rightTo = row(rCnt, rEnd - wd(rCnt))
                leftTo.forEach { vs(it, wTop, wBot) }
                for (i in 1 until rCnt) vs(rightTo[i], wTop, wBot) // 跳过i=0=挂一
                val hx = lerp(rightTo[0], cx, p); val hy = lerp(wCY, hangY, p)
                val hh = lerp(sh, sh * 0.22f, p)
                drawLine(Color(0xFF00FF00), Offset(hx, hy - hh / 2f), Offset(hx, hy + hh / 2f), sw)
            }

            // ── GROUP_LEFT：左堆每4根一组右移 ──
            YarrowPhase.GROUP_LEFT -> {
                val rCnt = n - num; val rightTo = row(rCnt, rEnd - wd(rCnt))
                val lFrom = row(num, lx)
                val lTo = g4(num, lx, false)
                for (i in 0 until num) vs(lerp(lFrom[i], lTo[i], p), wTop, wBot)
                // 右堆不动
                for (i in 1 until rCnt) vs(rightTo[i], wTop, wBot)
                val hh = sh * 0.22f; drawLine(Color(0xFF00FF00), Offset(cx, hangY - hh / 2f), Offset(cx, hangY + hh / 2f), sw)
            }

            // ── COLLECT_LEFT：左余缩短移到挂一左边 ──
            YarrowPhase.COLLECT_LEFT -> {
                val rCnt = n - num; val rightTo = row(rCnt, rEnd - wd(rCnt))
                val lGrp = g4(num, lx, false)
                val colXs = row(collected, stX(collected))
                val hiCol = ln // 挂一在归奇排中的索引=左余数

                for (i in 0 until num) {
                    if (i >= num - ln) { // 左余
                        val ci = i - (num - ln)
                        val x = lerp(lGrp[i], colXs[ci], p)
                        val y = lerp(wCY, hangY, p)
                        vh(x, y, lerp(sh, sh * 0.22f, p), 0.5f)
                    } else vs(lGrp[i], wTop, wBot)
                }
                // 右堆+挂一不动
                for (i in 1 until rCnt) vs(rightTo[i], wTop, wBot)
                val hh = sh * 0.22f
                drawLine(Color(0xFF00FF00), Offset(lerp(cx, colXs[hiCol], p), hangY - hh / 2f), Offset(lerp(cx, colXs[hiCol], p), hangY + hh / 2f), sw)
            }

            // ── GROUP_RIGHT：右堆每4根一组左移 ──
            YarrowPhase.GROUP_RIGHT -> {
                val lGrp = g4(num, lx, false)
                val colXs = row(collected, stX(collected))
                val hiCol = ln; val rw = n - num - 1
                val rFrom = row(rw, rEnd - wd(rw))
                val rTo = g4(rw, rEnd, true)

                // 左堆+左余(已归位)
                for (i in 0 until num) {
                    if (i >= num - ln) vh(colXs[i - (num - ln)], hangY, sh * 0.22f, 0.5f)
                    else vs(lGrp[i], wTop, wBot)
                }
                // 右堆分组
                for (i in 0 until rw) vs(lerp(rFrom[i], rTo[i], p), wTop, wBot)
                // 挂一
                vh(colXs[hiCol], hangY, sh * 0.22f, 1f)
            }

            // ── COLLECT_RIGHT：右余缩短移到挂一右边 ──
            YarrowPhase.COLLECT_RIGHT -> {
                val lGrp = g4(num, lx, false)
                val colXs = row(collected, stX(collected))
                val hiCol = ln; val rw = n - num - 1; val rGrp = g4(rw, rEnd, true)

                for (i in 0 until num) {
                    if (i >= num - ln) vh(colXs[i - (num - ln)], hangY, sh * 0.22f, 0.5f)
                    else vs(lGrp[i], wTop, wBot)
                }
                var ci = hiCol + 1
                for (i in 0 until rw) {
                    if (i < rn) { // 右余
                        val x = lerp(rGrp[i], colXs[ci], p)
                        val y = lerp(wCY, hangY, p); ci++
                        vh(x, y, lerp(sh, sh * 0.22f, p), 0.5f)
                    } else vs(rGrp[i], wTop, wBot)
                }
                vh(colXs[hiCol], hangY, sh * 0.22f, 1f)
            }

            // ── STORING：仅归奇伸长下移到b区。工作策（不含余数）保持分组 ──
            YarrowPhase.STORING -> {
                val lGrp = g4(num, lx, false); val rw = n - num - 1
                val rGrp = g4(rw, rEnd, true)
                val colXs = row(collected, stX(collected))
                val colTarget = row(collected, lx + state.bSize * step + sw / 2f)
                val lw2 = num - ln; val rw2 = rw - rn

                if (p < 0.5f) {
                    val p1 = p / 0.5f
                    // 工作策（不含余数）保持分组
                    for (i in 0 until lw2) vs(lGrp[i], wTop, wBot)
                    for (i in 0 until rw2) vs(rGrp[rn + i], wTop, wBot)
                    // 归奇在hang行
                    for (i in 0 until collected) vh(colXs[i], hangY, sh * 0.22f, 0.5f)
                } else {
                    val p2 = (p - 0.5f) / 0.5f
                    for (i in 0 until lw2) vs(lGrp[i], wTop, wBot)
                    for (i in 0 until rw2) vs(rGrp[rn + i], wTop, wBot)
                    for (i in 0 until collected) {
                        val x = lerp(colXs[i], colTarget[i], p2)
                        val y = lerp(hangY, colY, p2)
                        vh(x, y, lerp(sh * 0.22f, sh, p2), 0.4f)
                    }
                }
            }

            // ── FLASH_GROUPS(T=3)：分组状态闪红光数出6/7/8/9 ──
            YarrowPhase.FLASH_GROUPS -> {
                val lGrp = g4(num, lx, false); val rw = n - num - 1
                val rGrp = g4(rw, rEnd, true)
                val lw2 = num - ln; val rw2 = rw - rn
                // 工作策（不含余数）保持分组
                for (i in 0 until lw2) vs(lGrp[i], wTop, wBot)
                for (i in 0 until rw2) vs(rGrp[rn + i], wTop, wBot)
                // b区
                row(state.bSize, lx + sw / 2f).forEach { vs(it, colY - sh / 2f, colY + sh / 2f, 0.35f) }

                val lGs = (num - ln) / 4
                val rGs = (n - num - 1 - rn) / 4
                val totalGs = lGs + rGs
                if (totalGs > 0) {
                    val gIdx = (p * totalGs).toInt().coerceAtMost(totalGs - 1)
                    val gP = (p * totalGs - gIdx)
                    val alpha = (kotlin.math.sin(gP * kotlin.math.PI.toFloat()) * 0.6f).coerceIn(0f, 1f)

                    // 当前组的4根在分组位置中的起止索引（右组需跳过余数rn）
                    val xs4 = if (gIdx < lGs) {
                        val s = gIdx * 4; lGrp.slice(s..s + 3)
                    } else {
                        val s = rn + (gIdx - lGs) * 4; rGrp.slice(s..s + 3)
                    }
                    val gx1 = xs4.first() - sw / 2f - 2.dp.toPx()
                    val gx2 = xs4.last() + sw / 2f + 2.dp.toPx()
                    val gy1 = wTop - 8.dp.toPx()
                    val gy2 = wBot + 8.dp.toPx()
                    val glowR = 12.dp.toPx() * (1f + gP.toFloat())
                    drawRoundRect(Color.Red.copy(alpha = alpha * 0.3f),
                        Offset(gx1 - glowR, gy1 - glowR), Size(gx2 - gx1 + glowR * 2f, gy2 - gy1 + glowR * 2f),
                        CornerRadius(6.dp.toPx()))
                    drawRoundRect(Color.Red.copy(alpha = alpha),
                        Offset(gx1, gy1), Size(gx2 - gx1, gy2 - gy1),
                        CornerRadius(4.dp.toPx()), style = Stroke(2.dp.toPx()))
                }
            }

            // ── MERGING(T<=2)：工作策从分组位置汇聚中心点，然后散开 ──
            YarrowPhase.MERGING -> {
                // n已被advancePhase减少，num/ln/rn保留
                val oldN = n + ln + 1 + rn // 收集前的n
                val lGrp = g4(num, lx, false)
                val rWorkCnt = oldN - num - 1 // 右工作策(含余数)
                val rGrp = g4(rWorkCnt, rEnd, true)
                val lw2 = num - ln; val rw2 = rWorkCnt - rn
                val toXs = row(n, stX(n))

                if (p < 0.5f) {
                    val p1 = p / 0.5f
                    for (i in 0 until lw2) vs(lerp(lGrp[i], cx, p1), wTop, wBot)
                    for (i in 0 until rw2) vs(lerp(rGrp[rn + i], cx, p1), wTop, wBot)
                } else {
                    val p2 = (p - 0.5f) / 0.5f
                    for (i in 0 until n) vs(lerp(cx, toXs[i], p2), wTop, wBot)
                }
            }

            // ── LINE_END(T=3)：b→中心重叠, a分组→中心重叠, 全体散开 ──
            YarrowPhase.LINE_END -> {
                val lGrp = g4(num, lx, false); val rw = n - num - 1
                val rGrp = g4(rw, rEnd, true)
                val lw2 = num - ln; val rw2 = rw - rn
                val workN = lw2 + rw2
                val bTotal = 49 - workN
                val all49 = row(49, stX(49))
                val bFrom = row(bTotal, lx + sw / 2f)

                if (p < 0.33f) {
                    val p1 = p / 0.33f
                    // a保持分组不动
                    for (i in 0 until lw2) vs(lGrp[i], wTop, wBot)
                    for (i in 0 until rw2) vs(rGrp[rn + i], wTop, wBot)
                    // b→中心重叠
                    for (i in 0 until bTotal) {
                        val x = lerp(bFrom[i], cx, p1)
                        val y = lerp(colY, wCY, p1)
                        vs(x, lerp(colY - sh / 2f, wTop, p1), lerp(colY + sh / 2f, wBot, p1), 0.5f)
                    }
                } else if (p < 0.66f) {
                    val p2 = (p - 0.33f) / 0.33f
                    // a分组→中心重叠
                    for (i in 0 until lw2) vs(lerp(lGrp[i], cx, p2), wTop, wBot)
                    for (i in 0 until rw2) vs(lerp(rGrp[rn + i], cx, p2), wTop, wBot)
                    for (i in 0 until bTotal) vs(cx, wTop, wBot, 0.5f)
                } else {
                    val p3 = (p - 0.66f) / 0.34f
                    // 全体散开
                    for (i in 0 until 49) vs(lerp(cx, all49[i], p3), wTop, wBot)
                }
            }

            YarrowPhase.COMPLETE, YarrowPhase.REVELATION_READY -> {}
        }

        // === 爻线 ===
        for (i in state.lines.indices) {
            val ln2 = state.lines[i]; val by = lnB + (5 - i) * lnH; val lw2 = 70.dp.toPx()
            if (ln2.isYang) drawLine(Color.White, Offset(cx - lw2 / 2, by), Offset(cx + lw2 / 2, by), 3.dp.toPx())
            else { val g = 18.dp.toPx(); drawLine(Color.White, Offset(cx - lw2 / 2, by), Offset(cx - g / 2, by), 3.dp.toPx()); drawLine(Color.White, Offset(cx + g / 2, by), Offset(cx + lw2 / 2, by), 3.dp.toPx()) }
            if (ln2.isChanging) drawText(TM.measure(if (ln2.isYang) "○" else "×", style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Color(0xFFFFD700))),
                topLeft = Offset(cx + lw2 / 2 + 10.dp.toPx(), by - 12.dp.toPx()))
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
