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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.divination.YarrowPhase
import com.example.karma.ui.divination.YarrowUiState
import kotlin.math.min

@Composable
fun YarrowCanvas(
    state: YarrowUiState,
    onUserTap: (Float) -> Unit,
    onPhaseComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val D = LocalDensity.current; val TM = rememberTextMeasurer()
    val prog = remember { Animatable(0f) }
    val sw = with(D) { 3.dp.toPx() }; val sg = with(D) { 1.8.dp.toPx() }
    val step = sw + sg; val sh = with(D) { 90.dp.toPx() }
    val tjiY = with(D) { 60.dp.toPx() }; val tjiHalf = sh / 2f
    val wTop = with(D) { 180.dp.toPx() }; val wBot = wTop + sh; val wCY = (wTop + wBot) / 2f
    val hangY = with(D) { 400.dp.toPx() }
    val colY = with(D) { 520.dp.toPx() }
    val lnB = with(D) { 660.dp.toPx() }; val lnH = with(D) { 36.dp.toPx() }
    val mg = with(D) { 6.dp.toPx() }

    LaunchedEffect(state.phase) {
        val dur = when (state.phase) {
            YarrowPhase.INTRO -> 1200; YarrowPhase.SPLITTING -> 500; YarrowPhase.HANGING_ONE -> 400
            YarrowPhase.COUNTING_FOURS -> 600; YarrowPhase.COLLECTING -> 500; YarrowPhase.MERGING -> 600
            else -> 0
        }
        if (dur > 0) { prog.snapTo(0f); prog.animateTo(1f, tween(dur)); onPhaseComplete() }
    }

    Canvas(modifier = modifier.fillMaxSize()
        .pointerInput(state.phase) {
            if (state.phase == YarrowPhase.IDLE) detectTapGestures { onUserTap(it.x / size.width) }
        }
    ) {
        val cw = size.width; val cx = cw / 2f; val p = prog.value
        val total = state.totalSticks; val left = state.leftCount; val right = state.rightCount
        val rWork = right - 1; val hi = left
        val lr = state.leftRem; val rr = state.rightRem; val colN = 1 + lr + rr

        // === 辅助 ===
        fun wd(n: Int) = n * step - sg
        fun stX(n: Int) = cx - wd(n) / 2f
        fun row(n: Int, sx: Float) = (0 until n).map { sx + it * step + sw / 2f }
        fun vs(x: Float, top: Float, bot: Float, a: Float = 1f) {
            drawLine(Color(0xFF4488ff).copy(alpha = a), Offset(x, top), Offset(x, bot), sw)
        }
        fun vh(x: Float, cy: Float, h: Float, a: Float = 1f) {
            drawLine(Color(0xFF4488ff).copy(alpha = a), Offset(x, cy - h / 2f), Offset(x, cy + h / 2f), sw)
        }

        // 分组：rightToLeft=false → 左→右，余数在右；true → 右→左，余数在左
        fun g4(n: Int, refX: Float, rightToLeft: Boolean): List<Float> {
            if (n <= 0) return emptyList()
            val xs = FloatArray(n)
            if (rightToLeft) {
                // 从右端开始向左排，每4根一组，余数在最左
                var i = n - 1; var px = refX - sw / 2f  // 最右策中心
                while (i >= 0) {
                    val gs = min(4, i + 1)
                    for (j in gs - 1 downTo 0) { xs[i - j] = px - (gs - 1 - j) * step }
                    i -= gs
                    px -= gs * step + step * 1.5f  // 组间额外间距
                }
            } else {
                // 从左端开始向右排，每4根一组，余数在最右
                var i = 0; var px = refX + sw / 2f
                while (i < n) {
                    val gs = min(4, n - i)
                    for (j in 0 until gs) xs[i + j] = px + j * step
                    i += gs
                    px += gs * step + step * 1.5f
                }
            }
            return xs.toList()
        }

        // === 太极 ===
        if (state.phase != YarrowPhase.INTRO) {
            drawLine(Color(0xFF4488ff), Offset(cx - tjiHalf, tjiY), Offset(cx + tjiHalf, tjiY), 3.dp.toPx())
            drawText(TM.measure("太极", style = TextStyle(fontSize = 10.sp, color = Color(0xFF555577))),
                topLeft = Offset(cx - 12.dp.toPx(), tjiY - 18.dp.toPx()))
        }

        // === 归奇区历史累积（左下角） ===
        val acc = state.collectedHistory.sum()
        if (acc > 0 && state.phase != YarrowPhase.COMPLETE && state.phase != YarrowPhase.MERGING) {
            row(acc, mg + sw / 2f).forEach { vs(it, colY - sh / 2f, colY + sh / 2f, 0.35f) }
        }

        // 左右堆的基准坐标
        val lx = mg; val rEnd = cw - mg  // 左堆起点、右堆终点

        // ====== 各阶段 ======
        when (state.phase) {

            YarrowPhase.INTRO -> {
                val xs50 = row(50, stX(50)); val ch = 25
                val p1 = (p / 0.55f).coerceIn(0f, 1f); val p2 = ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
                for (i in 0 until 50) {
                    if (i == ch) drawLine(Color(0xFF4488ff), Offset(lerp(xs50[i], cx - tjiHalf, p1), lerp(wTop, tjiY, p1)),
                        Offset(lerp(xs50[i], cx + tjiHalf, p1), lerp(wBot, tjiY, p1)), sw)
                    else { val adj = if (i < ch) i else i - 1; vs(lerp(xs50[i], row(49, stX(49))[adj], p2), wTop, wBot) }
                }
                if (p > 0.4f) drawText(TM.measure("太极", style = TextStyle(fontSize = 10.sp, color = Color(0xFF555577).copy(alpha = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)))),
                    topLeft = Offset(cx - 12.dp.toPx(), tjiY - 18.dp.toPx()))
            }

            YarrowPhase.IDLE -> {
                row(total, stX(total)).forEach { vs(it, wTop, wBot) }
                drawText(TM.measure(if (state.lines.isEmpty()) "点击屏幕分策" else "第${state.changeNumber}变·第${state.lines.size + 1}爻·点击分策",
                    style = TextStyle(fontSize = 14.sp, color = Color(0xFF777777))), topLeft = Offset(cx - 100.dp.toPx(), wTop - 36.dp.toPx()))
            }

            // ① SPLITTING：左堆去左边，右堆去右边，挂一不动
            YarrowPhase.SPLITTING -> {
                val from = row(total, stX(total))
                val lTo = row(left, lx)
                val rTo = row(rWork, rEnd - wd(rWork))  // 右堆靠右
                for (i in 0 until left) vs(lerp(from[i], lTo[i], p), wTop, wBot)
                for (i in 0 until rWork) vs(lerp(from[hi + 1 + i], rTo[i], p), wTop, wBot)
                vs(from[hi], wTop, wBot)
            }

            // ② HANGING_ONE：挂一缩短下移
            YarrowPhase.HANGING_ONE -> {
                val lXs = row(left, lx)
                val rXs = row(rWork, rEnd - wd(rWork))
                lXs.forEach { vs(it, wTop, wBot) }; rXs.forEach { vs(it, wTop, wBot) }
                val fromX = row(total, stX(total))[hi]
                val hh = sh * 0.22f
                drawLine(Color(0xFF4488ff), Offset(lerp(fromX, cx, p), lerp(wCY, hangY, p) - hh / 2f),
                    Offset(lerp(fromX, cx, p), lerp(wCY, hangY, p) + hh / 2f), sw)
            }

            // ③ COUNTING_FOURS：左向右分，右向左分
            YarrowPhase.COUNTING_FOURS -> {
                val lFrom = row(left, lx); val lTo = g4(left, lx, false)
                val rFrom = row(rWork, rEnd - wd(rWork)); val rTo = g4(rWork, rEnd, true)
                for (i in 0 until left) vs(lerp(lFrom[i], lTo[i], p), wTop, wBot)
                for (i in 0 until rWork) vs(lerp(rFrom[i], rTo[i], p), wTop, wBot)
                vh(cx, hangY, sh * 0.22f)
            }

            // ④ COLLECTING：余数缩短移到挂一同行
            YarrowPhase.COLLECTING -> {
                val lFrom = g4(left, lx, false); val rFrom = g4(rWork, rEnd, true)
                val colXs = row(colN, stX(colN))  // 挂一居中，余数在两侧
                val hiInCol = lr  // 挂一在归奇排中的位置

                // 左堆：后 lr 根是余数 → 移到归奇排左侧
                for (i in 0 until left) {
                    val isR = i >= left - lr
                    val y = if (isR) lerp(wCY, hangY, p) else wCY
                    val h = if (isR) lerp(sh, sh * 0.22f, p) else sh
                    val x = if (isR) lerp(lFrom[i], colXs[i - (left - lr)], p) else lFrom[i]
                    if (isR) vh(x, y, h, 0.5f) else vh(x, y, h)
                }
                // 右堆工作策：前 rr 根是余数（最左边）→ 移到归奇排右侧
                var ci = hiInCol + 1
                for (i in 0 until rWork) {
                    val isR = i < rr  // 右堆余数在最左边几根
                    val y = if (isR) lerp(wCY, hangY, p) else wCY
                    val h = if (isR) lerp(sh, sh * 0.22f, p) else sh
                    val x = if (isR) lerp(rFrom[i], colXs[ci], p) else rFrom[i]
                    if (isR) { vh(x, y, h, 0.5f); ci++ } else vh(x, y, h)
                }
                // 挂一
                vh(lerp(cx, colXs[hiInCol], p), hangY, sh * 0.22f, lerp(1f, 0.5f, p))
            }

            // ⑤ MERGING：归奇堆伸长下移左边 + 工作策合并回中央
            YarrowPhase.MERGING -> {
                val colXs = row(colN, stX(colN))
                val colL = row(colN, mg + sw / 2f)
                for (i in 0 until colN) { val x = lerp(colXs[i], colL[i], p); val y = lerp(hangY, colY, p); vh(x, y, lerp(sh * 0.22f, sh, p), 0.4f) }
                // 工作策：从左右两边合并回中央
                val rem = state.remainingSticks; val toXs = row(rem, stX(rem))
                val lwCnt = left - lr; val rwCnt = rWork - rr
                for (i in 0 until lwCnt) vs(lerp(row(left, lx)[i], toXs[i], p), wTop, wBot)
                for (i in 0 until rwCnt) vs(lerp(row(rWork, rEnd - wd(rWork))[rr + i], toXs[lwCnt + i], p), wTop, wBot)
            }

            YarrowPhase.COMPLETE, YarrowPhase.LINE_RESULT -> {}
        }

        // === 爻线 ===
        for (i in state.lines.indices) {
            val ln = state.lines[i]; val by = lnB + (5 - i) * lnH; val lw = 70.dp.toPx()
            if (ln.isYang) drawLine(Color.White, Offset(cx - lw / 2, by), Offset(cx + lw / 2, by), 3.dp.toPx())
            else { val g = 18.dp.toPx(); drawLine(Color.White, Offset(cx - lw / 2, by), Offset(cx - g / 2, by), 3.dp.toPx()); drawLine(Color.White, Offset(cx + g / 2, by), Offset(cx + lw / 2, by), 3.dp.toPx()) }
            if (ln.isChanging) drawText(TM.measure(if (ln.isYang) "○" else "×", style = TextStyle(fontSize = 18.sp, color = Color(0xFFFFD700))),
                topLeft = Offset(cx + lw / 2 + 10.dp.toPx(), by - 12.dp.toPx()))
            drawText(TM.measure(when (i) { 0 -> "初"; 1 -> "二"; 2 -> "三"; 3 -> "四"; 4 -> "五"; 5 -> "上"; else -> "" },
                style = TextStyle(fontSize = 11.sp, color = Color(0xFF666666))), topLeft = Offset(cx - lw / 2 - 18.dp.toPx(), by - 8.dp.toPx()))
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
