package com.example.karma.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.karma.ui.theme.ChartBg

private const val PAD_TOP = 20f
private const val PAD_RIGHT = 20f
private const val PAD_BOTTOM = 40f
private const val PAD_LEFT = 60f

class ChartViewport(
    var viewStart: Double = 0.0,
    var viewEnd: Double = 0.0,
    var yMin: Float = 0f,
    var yMax: Float = 10f,
    var minTimeRange: Double = 3600000.0,  // 1 hour minimum
    var maxTimeRange: Double = 0.0,        // set on first auto-fit
)

@Composable
fun HistoryChartCanvas(
    points: List<AggregatedPoint>,
    viewport: ChartViewport,
    lineThickness: Float = 2f,
    dotRadius: Float = 3.5f,
    ranks: List<com.example.karma.data.model.Rank> = emptyList(),
    onPointClicked: ((point: AggregatedPoint?, screenX: Float, screenY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currentOnPointClicked by rememberUpdatedState(onPointClicked)
    val currentPoints by rememberUpdatedState(points)

    // Pre-allocated Paint and Calendar for reuse inside Canvas draw
    val placeholderPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(77, 255, 255, 255)
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val gridLabelPaint = remember {
        android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.RIGHT }
    }
    val timeLabelPaint = remember {
        android.graphics.Paint().apply { textAlign = android.graphics.Paint.Align.CENTER }
    }
    val cal = remember { java.util.Calendar.getInstance() }

    // ---- Gesture: tap to select point ----
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(viewport) {
                detectTapGestures { offset ->
                    val callback = currentOnPointClicked
                    val pts = currentPoints
                    if (callback == null || pts.size < 1) return@detectTapGestures
                    val tapX = offset.x
                    val tapY = offset.y
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val plotW = w - PAD_LEFT - PAD_RIGHT
                    val plotH = h - PAD_TOP - PAD_BOTTOM
                    if (plotW <= 0f || plotH <= 0f) return@detectTapGestures

                    val vp = viewport
                    val nearest = pts.minByOrNull { point ->
                        val px = PAD_LEFT + ((point.timestamp - vp.viewStart) / (vp.viewEnd - vp.viewStart) * plotW).toFloat()
                        val py = PAD_TOP + (1f - (point.totalAfter - vp.yMin) / (vp.yMax - vp.yMin)) * plotH
                        val dx = px - tapX
                        val dy = py - tapY
                        dx * dx + dy * dy
                    }
                    if (nearest != null) {
                        val px = PAD_LEFT + ((nearest.timestamp - vp.viewStart) / (vp.viewEnd - vp.viewStart) * plotW).toFloat()
                        val py = PAD_TOP + (1f - (nearest.totalAfter - vp.yMin) / (vp.yMax - vp.yMin)) * plotH
                        val distSq = (px - tapX) * (px - tapX) + (py - tapY) * (py - tapY)
                        val maxDist = 120f
                        if (distSq <= maxDist * maxDist) {
                            callback(nearest, px, py)
                        } else {
                            callback(null, 0f, 0f)
                        }
                    } else {
                        callback(null, 0f, 0f)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0 || points.isEmpty()) {
            drawContext.canvas.nativeCanvas.drawText(
                if (points.isEmpty()) "暂无足够数据" else "",
                w / 2f, h / 2f,
                placeholderPaint,
            )
            return@Canvas
        }

        val plotW = w - PAD_LEFT - PAD_RIGHT
        val plotH = h - PAD_TOP - PAD_BOTTOM

        val vp = viewport
        // ---- Auto-fit viewport on first draw ----
        if (vp.viewEnd <= vp.viewStart) {
            val timeRange = points.last().timestamp - points.first().timestamp
            if (timeRange == 0L) {
                // 单点：居中显示，左右各 2 小时
                vp.viewStart = points.first().timestamp - 7200000.0
                vp.viewEnd = points.first().timestamp + 7200000.0
                vp.minTimeRange = 3600000.0
                vp.maxTimeRange = 14400000.0
            } else {
                val padding = 0.1
                vp.viewStart = points.first().timestamp - (timeRange * padding)
                vp.viewEnd = points.last().timestamp + (timeRange * padding)
                if (vp.viewEnd <= vp.viewStart) vp.viewEnd = vp.viewStart + 3600000.0
                vp.minTimeRange = minOf(3600000.0, maxOf(timeRange / 20.0, 600000.0))
                vp.maxTimeRange = vp.viewEnd - vp.viewStart
            }

            val scores = points.map { it.totalAfter }
            val yMin = scores.min()
            val yMax = scores.max()
            val yPad = maxOf((yMax - yMin) * 0.15f, 5f)
            vp.yMin = yMin - yPad
            vp.yMax = yMax + yPad
        }

        // Mapping functions
        val xMap: (Long) -> Float = { t ->
            PAD_LEFT + ((t - vp.viewStart) / (vp.viewEnd - vp.viewStart) * plotW).toFloat()
        }
        val yMap: (Float) -> Float = { s ->
            PAD_TOP + (1f - (s - vp.yMin) / (vp.yMax - vp.yMin)) * plotH
        }

        // ---- Background ----
        drawRect(color = ChartBg, size = size)

        // ---- Rank color bands（阶位色带） ----
        if (ranks.isNotEmpty() && points.size >= 2) {
            for (rank in ranks) {
                val bandColor = rank.colorHex

                val bandTopY = yMap(rank.max)
                val bandBottomY = yMap(rank.min)

                val drawTop = bandTopY.coerceIn(PAD_TOP, h - PAD_BOTTOM)
                val drawBottom = bandBottomY.coerceIn(PAD_TOP, h - PAD_BOTTOM)
                val bandHeight = drawBottom - drawTop
                if (bandHeight <= 0f) continue

                drawRect(
                    color = Color(bandColor).copy(alpha = 0.10f),
                    topLeft = Offset(PAD_LEFT, drawTop),
                    size = androidx.compose.ui.geometry.Size(plotW, bandHeight),
                )
            }
        }

        // ---- Grid lines ----
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = PAD_TOP + (i.toFloat() / gridLines) * plotH
            drawLine(
                color = Color.White.copy(alpha = 0.06f),
                start = Offset(PAD_LEFT, y),
                end = Offset(w - PAD_RIGHT, y),
                strokeWidth = 1f,
            )
            val score = vp.yMax - (i.toFloat() / gridLines) * (vp.yMax - vp.yMin)
            gridLabelPaint.apply {
                color = android.graphics.Color.argb(89, 255, 255, 255)
                textSize = 22f
            }
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", score),
                PAD_LEFT - 6f,
                y + 4f,
                gridLabelPaint,
            )
        }

        // ---- Time labels ----
        val timeLabels = 5
        for (i in 0..timeLabels) {
            val t = vp.viewStart + (i.toDouble() / timeLabels) * (vp.viewEnd - vp.viewStart)
            val x = xMap(t.toLong())
            if (x < PAD_LEFT || x > w - PAD_RIGHT) continue
            cal.timeInMillis = t.toLong()
            val label = "${cal.get(java.util.Calendar.MONTH) + 1}/${cal.get(java.util.Calendar.DATE)}"
            timeLabelPaint.apply {
                color = android.graphics.Color.argb(89, 255, 255, 255)
                textSize = 20f
            }
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                h - PAD_BOTTOM + 16f,
                timeLabelPaint,
            )
        }

        // ---- Data line ----
        val linePath = Path()
        for (i in points.indices) {
            val x = xMap(points[i].timestamp)
            val y = yMap(points[i].totalAfter)
            if (x < PAD_LEFT - 10f || x > w - PAD_RIGHT + 10f) continue
            if (i == 0) linePath.moveTo(x, y)
            else linePath.lineTo(x, y)
        }
        drawPath(linePath, color = Color(0xFFffd700), style = Stroke(width = lineThickness))

        // ---- Dots ----
        val maxDots = 200
        val step = maxOf(1, points.size / maxDots)
        for (i in points.indices step step) {
            val x = xMap(points[i].timestamp)
            val y = yMap(points[i].totalAfter)
            if (x < PAD_LEFT - 10f || x > w - PAD_RIGHT + 10f) continue
            drawCircle(color = Color(0xFFffd700), radius = dotRadius, center = Offset(x, y))
            drawCircle(
                color = ChartBg,
                radius = dotRadius,
                center = Offset(x, y),
                style = Stroke(width = 1.5f),
            )
        }
    }
}
