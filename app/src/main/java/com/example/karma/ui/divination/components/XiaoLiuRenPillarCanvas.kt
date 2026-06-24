package com.example.karma.ui.divination.components

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.karma.R
import com.example.karma.ui.divination.model.XiaoLiuRenPalaces
import kotlinx.coroutines.delay

/**
 * 六宫在屏幕上的坐标数据（供金线动画使用）
 */
data class PalacePosition(
    val index: Int,
    val centerX: Float,
    val centerY: Float,
)

/** 图片固有尺寸（像素） */
private const val IMG_W = 1535f
private const val IMG_H = 2620f

/** 图片中三柱 X 中心坐标 */
private val IMG_PILLAR_CX = floatArrayOf(346f, 763f, 1179f)

/** 图片中上三段（留连/速喜/赤口）的上下边界 */
private const val IMG_UPPER_TOP = 963f
private const val IMG_UPPER_BOTTOM = 1285f

/** 图片中下三段（大安/空亡/小吉）的上下边界 */
private const val IMG_LOWER_TOP = 1419f
private const val IMG_LOWER_BOTTOM = 1798f

/** 图片中柱子平均半宽 */
private const val IMG_PILLAR_HALF_W = 116f

/** 图片底部需要裁剪掉的空白像素（0 = 不裁剪，铺满全画布） */
private const val IMG_CROP_BOTTOM = 0f

@Composable
fun XiaoLiuRenPillarCanvas(
    onPalacePositionsReady: (List<PalacePosition>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 呼吸动画时间驱动
    val breathTime = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            breathTime.floatValue += 0.016f
        }
    }

    // 加载柱子背景图片
    val context = LocalContext.current
    val pillarImage = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.pillar_bg).asImageBitmap()
    }

    var positionsReady by remember { mutableStateOf(false) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height
        val breath = breathTime.floatValue

        // 图片拉伸铺满画布（上下接触顶边和底边）
        val scaleX = screenW / IMG_W
        val contentH = IMG_H - IMG_CROP_BOTTOM
        val scaleY = screenH / contentH

        // 绘制柱子背景图片（裁剪底部空白后拉伸铺满）
        drawImage(
            image = pillarImage,
            srcOffset = IntOffset(0, 0),
            srcSize = IntSize(IMG_W.toInt(), contentH.toInt()),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(screenW.toInt(), screenH.toInt()),
        )

        // 图片坐标 → Canvas 坐标（X/Y 独立缩放）
        val pillarCx = IMG_PILLAR_CX.map { it * scaleX }
        val upperCy = (IMG_UPPER_TOP + IMG_UPPER_BOTTOM) / 2f * scaleY
        val lowerCy = (IMG_LOWER_TOP + IMG_LOWER_BOTTOM) / 2f * scaleY
        val pillarHw = IMG_PILLAR_HALF_W * scaleX
        val upperHh = (IMG_UPPER_BOTTOM - IMG_UPPER_TOP) / 2f * scaleY
        val lowerHh = (IMG_LOWER_BOTTOM - IMG_LOWER_TOP) / 2f * scaleY

        // 六宫中心坐标（供金线动画使用）
        val positions = listOf(
            PalacePosition(0, pillarCx[0], lowerCy),       // 大安 — 左下
            PalacePosition(1, pillarCx[0], upperCy),       // 留连 — 左上
            PalacePosition(2, pillarCx[1], upperCy),       // 速喜 — 中上
            PalacePosition(3, pillarCx[2], upperCy),       // 赤口 — 右上
            PalacePosition(4, pillarCx[2], lowerCy),       // 小吉 — 右下
            PalacePosition(5, pillarCx[1], lowerCy),       // 空亡 — 中下
        )

        if (!positionsReady) {
            positionsReady = true
            onPalacePositionsReady(positions)
        }

        // 背景漂浮粒子
        drawBackgroundParticles(breath, screenW, screenH)

        // 三柱六宫：每柱上下两段（图片已绘制花纹，这里仅叠加文本和高亮）
        val upperIndices = intArrayOf(1, 2, 3)   // 留连, 速喜, 赤口
        val lowerIndices = intArrayOf(0, 5, 4)   // 大安, 空亡, 小吉

        for (i in 0 until 3) {
            // 上段
            val upperRect = Rect(
                pillarCx[i] - pillarHw, upperCy - upperHh,
                pillarCx[i] + pillarHw, upperCy + upperHh,
            )
            val upperPalace = XiaoLiuRenPalaces.getPalaceByIndex(upperIndices[i])
            drawPillarSegment(
                rect = upperRect, palaceName = upperPalace.name, sixGods = upperPalace.sixGods,
            )

            // 下段
            val lowerRect = Rect(
                pillarCx[i] - pillarHw, lowerCy - lowerHh,
                pillarCx[i] + pillarHw, lowerCy + lowerHh,
            )
            val lowerPalace = XiaoLiuRenPalaces.getPalaceByIndex(lowerIndices[i])
            drawPillarSegment(
                rect = lowerRect, palaceName = lowerPalace.name, sixGods = lowerPalace.sixGods,
            )
        }
    }
}

/** 背景漂浮金粒子 */
private fun DrawScope.drawBackgroundParticles(breath: Float, screenW: Float, screenH: Float) {
    val particles = listOf(
        0.13f to 0.27f, 0.37f to 0.15f, 0.62f to 0.33f, 0.81f to 0.22f, 0.25f to 0.71f,
        0.55f to 0.62f, 0.73f to 0.78f, 0.07f to 0.55f, 0.88f to 0.64f, 0.44f to 0.85f,
        0.18f to 0.42f, 0.68f to 0.48f, 0.35f to 0.55f, 0.92f to 0.41f, 0.51f to 0.17f,
    )
    for ((i, p) in particles.withIndex()) {
        val phase = breath * 0.3f + i * 0.7f
        val alpha = (kotlin.math.sin(phase) * 0.5f + 0.5f) * 0.07f
        val yOffset = kotlin.math.sin(phase * 1.3f) * 15.dp.toPx()
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = alpha),
            radius = 1.5.dp.toPx() + (i % 3) * 0.5.dp.toPx(),
            center = Offset(p.first * screenW, p.second * screenH + yOffset),
        )
    }
}

/** 绘制单段柱体的叠加效果（图片之上） */
private fun DrawScope.drawPillarSegment(
    rect: Rect,
    palaceName: String,
    sixGods: String,
) {
    // 宫名标签（古铜金色粗体）
    val namePaint = Paint().apply {
        color = 0xFFdaa520.toInt()
        textSize = 18.dp.toPx()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        palaceName, rect.center.x, rect.center.y + 3.dp.toPx(), namePaint,
    )

    // 3. 六神小标签
    val godPaint = Paint().apply {
        color = 0xCCffd700.toInt()
        textSize = 10.dp.toPx()
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        sixGods, rect.center.x, rect.center.y + 22.dp.toPx(), godPaint,
    )
}
