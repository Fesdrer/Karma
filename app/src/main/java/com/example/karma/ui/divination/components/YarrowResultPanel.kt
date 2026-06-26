package com.example.karma.ui.divination.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.divination.model.IntegrationResult
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
                    primaryUpper = primary.upperTrigram.unicode,
                    primaryLower = primary.lowerTrigram.unicode,
                    primaryUpperName = primary.upperTrigram.label,
                    primaryUpperElem = primary.upperTrigram.element,
                    primaryLowerName = primary.lowerTrigram.label,
                    primaryLowerElem = primary.lowerTrigram.element,
                    transformedName = transformed.fullName,
                    transformedUpper = transformed.upperTrigram.unicode,
                    transformedLower = transformed.lowerTrigram.unicode,
                    transformedUpperName = transformed.upperTrigram.label,
                    transformedUpperElem = transformed.upperTrigram.element,
                    transformedLowerName = transformed.lowerTrigram.label,
                    transformedLowerElem = transformed.lowerTrigram.element,
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

/** 本卦 ｜ 变卦 双栏显示 */
@Composable
private fun HexagramPairDisplay(
    primaryName: String,
    primaryUpper: String,
    primaryLower: String,
    primaryUpperName: String,
    primaryUpperElem: String,
    primaryLowerName: String,
    primaryLowerElem: String,
    transformedName: String,
    transformedUpper: String,
    transformedLower: String,
    transformedUpperName: String,
    transformedUpperElem: String,
    transformedLowerName: String,
    transformedLowerElem: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        // 本卦（左）
        HexagramCard(
            label = "本卦",
            name = primaryName,
            upperTrigram = primaryUpper,
            lowerTrigram = primaryLower,
            upperText = "$primaryUpperName $primaryUpperElem",
            lowerText = "$primaryLowerName $primaryLowerElem",
        )

        // 变卦（右）
        HexagramCard(
            label = "变卦",
            name = transformedName,
            upperTrigram = transformedUpper,
            lowerTrigram = transformedLower,
            upperText = "$transformedUpperName $transformedUpperElem",
            lowerText = "$transformedLowerName $transformedLowerElem",
        )
    }
}

/** 单栏卦象卡片 */
@Composable
private fun HexagramCard(
    label: String,
    name: String,
    upperTrigram: String,
    lowerTrigram: String,
    upperText: String,
    lowerText: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        // 外框
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PanelBg)
                .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 上卦
                Text(upperTrigram, fontSize = 32.sp, color = Gold)
                Spacer(Modifier.height(2.dp))
                Text(upperText, fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                // 间隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(1.dp)
                        .background(Gold.copy(alpha = 0.3f))
                )
                Spacer(Modifier.height(8.dp))
                // 下卦
                Text(lowerTrigram, fontSize = 32.sp, color = Gold)
                Spacer(Modifier.height(2.dp))
                Text(lowerText, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Gold)
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
