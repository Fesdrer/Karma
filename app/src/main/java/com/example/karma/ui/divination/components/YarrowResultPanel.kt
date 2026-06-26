package com.example.karma.ui.divination.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.divination.model.IntegrationResult
import com.example.karma.ui.divination.model.Trigram
import com.example.karma.ui.divination.model.YarrowResult
import com.example.karma.ui.theme.ChartBg
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.PanelBg
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

@Composable
fun YarrowResultPanel(
    result: YarrowResult,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = result.primaryHexagram ?: return
    val transformed = result.transformedHexagram ?: return
    val integration = result.integration ?: return

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(400)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(400)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ChartBg)
                    .border(1.5.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ===== 标题 =====
                Text("【 大 衍 筮 法 】", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))

                // ===== 第一块：本卦 ｜ 变卦 =====
                HexagramPairDisplay(
                    primaryName = primary.fullName,
                    primaryShortName = primary.name,
                    primaryLowerTrig = primary.lowerTrigram,
                    primaryUpperTrig = primary.upperTrigram,
                    transformedName = transformed.fullName,
                    transformedShortName = transformed.name,
                    transformedLowerTrig = transformed.lowerTrigram,
                    transformedUpperTrig = transformed.upperTrigram,
                )

                Spacer(Modifier.height(10.dp))

                // 能量趋势
                TrendDisplay(integration)

                Spacer(Modifier.height(14.dp))

                // ===== 第二块：卦象总纲 =====
                SectionBorder {
                    Column {
                        Text("【卦象总纲】", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = primary.summary,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ===== 第三块：六爻精解 =====
                SectionBorder {
                    Column {
                        val movingSet = result.movingLines.toSet()
                        Text("【六爻精解】（★为动爻，重点关注）", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
                        Spacer(Modifier.height(8.dp))
                        for (i in 0..5) {
                            val isMoving = movingSet.contains(i + 1)
                            val star = if (isMoving) " ★" else ""
                            val revText = primary.revelations.getOrElse(i) { "" }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = revText + star,
                                    fontSize = 13.sp,
                                    color = if (isMoving) Gold else TextPrimary,
                                    lineHeight = 20.sp,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ===== 第四块：变卦指向总结 =====
                SectionBorder {
                    Text(
                        text = "【变卦指向】变卦为${transformed.fullName}，提示最终外部环境将趋于${transformed.trend}，建议结合上述动爻焦点，权衡进退。",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp,
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ===== 再来一次按钮 =====
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb8860b), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(0.6f).height(44.dp),
                ) {
                    Text("再来一次", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ==================== 子组件 ====================

/** 从三画卦的数值值转换为六爻阴阳列表（下到上） */
private fun trigramValueToLines(lowerVal: Int, upperVal: Int): List<Boolean> {
    return listOf(
        (lowerVal and 0b001) != 0,   // 初爻
        (lowerVal and 0b010) != 0,   // 二爻
        (lowerVal and 0b100) != 0,   // 三爻
        (upperVal and 0b001) != 0,   // 四爻
        (upperVal and 0b010) != 0,   // 五爻
        (upperVal and 0b100) != 0,   // 上爻
    )
}

/** 本卦 ｜ 变卦 双栏显示 */
@Composable
private fun HexagramPairDisplay(
    primaryName: String,
    primaryShortName: String,
    primaryLowerTrig: Trigram,
    primaryUpperTrig: Trigram,
    transformedName: String,
    transformedShortName: String,
    transformedLowerTrig: Trigram,
    transformedUpperTrig: Trigram,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        HexagramView(
            label = "本卦",
            lines = trigramValueToLines(primaryLowerTrig.value, primaryUpperTrig.value),
            hexagramName = primaryShortName,
            fullName = primaryName,
            upperLabel = primaryUpperTrig.label,
            upperElem = primaryUpperTrig.element,
            lowerLabel = primaryLowerTrig.label,
            lowerElem = primaryLowerTrig.element,
        )

        HexagramView(
            label = "变卦",
            lines = trigramValueToLines(transformedLowerTrig.value, transformedUpperTrig.value),
            hexagramName = transformedShortName,
            fullName = transformedName,
            upperLabel = transformedUpperTrig.label,
            upperElem = transformedUpperTrig.element,
            lowerLabel = transformedLowerTrig.label,
            lowerElem = transformedLowerTrig.element,
        )
    }
}

/** 用 Canvas 画一个卦（6条线堆叠） */
@Composable
private fun HexagramView(
    label: String,
    lines: List<Boolean>,   // 6个，从初爻到上爻
    hexagramName: String,
    fullName: String,
    upperLabel: String,
    upperElem: String,
    lowerLabel: String,
    lowerElem: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 标题
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(Modifier.height(4.dp))

        // 六爻方块
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 124.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PanelBg)
                .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 88.dp, height = 108.dp),
            ) {
                drawHexagram(lines)
            }
        }

        Spacer(Modifier.height(4.dp))

        // 名称
        Text("$hexagramName（$fullName）", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(2.dp))

        // 上卦/下卦信息
        Text("上卦：$upperLabel $upperElem", fontSize = 11.sp, color = TextSecondary)
        Text("下卦：$lowerLabel $lowerElem", fontSize = 11.sp, color = TextSecondary)
    }
}

/** 在 Canvas 内绘制六条爻线 */
private fun DrawScope.drawHexagram(lines: List<Boolean>) {
    val w = size.width
    val h = size.height
    val gapRatio = 0.28f          // 阴线缺口占线宽的比例
    val lineThickness = h / 16f   // 线粗
    val topGap = lineThickness * 1.0f   // 顶部留白
    val lineSpan = (h - topGap * 2) / 5f   // 6条线跨越5个间隔
    val sidePad = w * 0.08f       // 左右留白
    val lineWidth = w - sidePad * 2 // 画线总宽
    val cornerR = CornerRadius(lineThickness * 0.4f)
    val lineColor = Gold

    for (i in 0..5) {
        val y = topGap + (5 - i) * lineSpan  // i=5(上爻)在最上面
        val isYang = if (i < lines.size) lines[i] else true

        if (isYang) {
            // 阳爻：一整条
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(sidePad, y - lineThickness / 2),
                size = Size(lineWidth, lineThickness),
                cornerRadius = cornerR,
            )
        } else {
            // 阴爻：两段，中间有缺口
            val halfW = lineWidth * (1f - gapRatio) / 2f
            val gapW = lineWidth * gapRatio
            // 左段
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(sidePad, y - lineThickness / 2),
                size = Size(halfW, lineThickness),
                cornerRadius = cornerR,
            )
            // 右段
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(sidePad + halfW + gapW, y - lineThickness / 2),
                size = Size(halfW, lineThickness),
                cornerRadius = cornerR,
            )
        }
    }
}

/** 能量趋势 */
@Composable
private fun TrendDisplay(integration: IntegrationResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PanelBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "【能量趋势】",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "${integration.energyFlow}，${integration.trendDesc}",
            fontSize = 13.sp,
            color = TextPrimary,
        )
    }
}

/** 带边框的区块容器 */
@Composable
private fun SectionBorder(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PanelBg)
            .border(1.dp, Gold.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        content()
    }
}
