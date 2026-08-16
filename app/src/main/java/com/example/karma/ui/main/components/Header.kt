package com.example.karma.ui.main.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.data.model.Rank
import com.example.karma.ui.theme.Gold

@Composable
fun Header(
    totalScore: Float,
    rank: Rank?,
    ranks: List<Rank> = emptyList(),
    luckValue: Float? = null,
    modifier: Modifier = Modifier,
) {
    // 分数变化弹跳（Apple 成就感时刻）：变化瞬间放大，弹簧回弹。
    // 首次组合只记录不弹；后续每次分数变化触发一次弹跳。
    val scoreScale = remember { Animatable(1f) }
    var lastScore by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(totalScore) {
        if (lastScore != null && totalScore != lastScore) {
            scoreScale.snapTo(1.2f)
            scoreScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
            )
        }
        lastScore = totalScore
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (totalScore == totalScore.toInt().toFloat()) {
                        totalScore.toInt().toString()
                    } else {
                        String.format("%.1f", totalScore)
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalScore < 0) {
                        Color(0xFFff5252)
                    } else {
                        Color(0xFFffd700)
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scoreScale.value
                        scaleY = scoreScale.value
                    },
                )

                if (rank != null) {
                    Spacer(Modifier.width(10.dp))
                    val totalRanks = ranks.size
                    val textColor = if (totalRanks > 0 && rank.level <= totalRanks * 2 / 3)
                        Color.White else Color(0xFFffd700)
                    // 正阶徽章半透明底；负阶纯色显示（默认纯黑，40% 透明度会视觉上变成灰色）
                    val bgColor = if (rank.level > 0) {
                        Color(rank.colorHex).copy(alpha = 0.4f)
                    } else {
                        Color(rank.colorHex)
                    }
                    Text(
                        text = rank.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                    )
                }
            }

            // 运气增幅值 — 小字、不喧宾夺主
            if (luckValue != null) {
                Spacer(Modifier.height(2.dp))
                val luckText = if (luckValue >= 0) {
                    "运气 +" + String.format("%.2f", luckValue)
                } else {
                    "运气 " + String.format("%.2f", luckValue)
                }
                Text(
                    text = luckText,
                    fontSize = 12.sp,
                    color = when {
                        luckValue > 0f -> Color(0xFF66bb6a)
                        luckValue < 0f -> Color(0xFFef5350)
                        else -> Color(0xFF888888)
                    },
                )
            }
        }
    }
}
