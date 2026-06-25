package com.example.karma.ui.divination.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.karma.ui.divination.model.HexagramLine
import com.example.karma.ui.divination.model.YarrowResult
import com.example.karma.ui.theme.ChartBg
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.GreenPositive
import com.example.karma.ui.theme.PanelBg
import com.example.karma.ui.theme.RedNegative
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

@Composable
fun YarrowResultPanel(
    result: YarrowResult,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    .fillMaxWidth(0.88f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ChartBg)
                    .border(1.5.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("【 大 衍 筮 法 】", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))

                // 六爻展示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelBg)
                        .border(1.dp, Gold.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("本卦", fontSize = 16.sp, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        // 从上爻到初爻（从上往下显示）
                        for (i in result.lines.indices.reversed()) {
                            val line = result.lines[i]
                            val label = when (i) { 0 -> "初"; 1 -> "二"; 2 -> "三"; 3 -> "四"; 4 -> "五"; 5 -> "上" else -> "" }
                            val color = if (line.isChanging) {
                                if (line.isYang) Color(0xFFff9800) else Color(0xFFff5252)
                            } else {
                                TextPrimary
                            }
                            Text(
                                text = "$label" + "爻  " + line.label,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 变爻提示
                val changingLines = result.lines.filter { it.isChanging }
                if (changingLines.isNotEmpty()) {
                    Text(
                        text = "变爻：${changingLines.size} 爻变",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 启示区域（留空）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PanelBg)
                        .border(1.dp, Gold.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("卦辞启示 · 即将推出", fontSize = 15.sp, color = TextSecondary)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb8860b), contentColor = Color.White),
                    modifier = Modifier.height(44.dp),
                ) {
                    Text("再来一次", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
