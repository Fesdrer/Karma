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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.divination.model.PalaceRevelation
import com.example.karma.ui.theme.BlueLight
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ChartBg
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.GreenPositive
import com.example.karma.ui.theme.PanelBg
import com.example.karma.ui.theme.RedNegative
import com.example.karma.ui.theme.ScoreBtnBg
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

@Composable
fun XiaoLiuRenResultPanel(
    result: PalaceRevelation,
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
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(enabled = false) {}, // 阻止点击穿透
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .fillMaxHeight(0.82f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ChartBg)
                    .border(1.5.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 标题
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Gold.copy(alpha = 0.08f))
                        .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "【 占 卜 结 果 】",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 宫名卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelBg)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = result.name,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                        )
                        Spacer(Modifier.height(6.dp))

                        // 吉凶徽章
                        val isAuspicious = result.fortuneLevel.isAuspicious
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isAuspicious) GreenPositive.copy(alpha = 0.15f)
                                    else RedNegative.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = result.fortuneLevel.label,
                                color = if (isAuspicious) GreenPositive else RedNegative,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))

                        // 五行 · 方位 · 六神
                        Text(
                            text = "${result.wuxing} · ${result.direction} · ${result.sixGods}",
                            fontSize = 14.sp,
                            color = BlueLight,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 五项启示
                RevelationSection("运势", result.fortune)
                RevelationSection("财富", result.wealth)
                RevelationSection("感情", result.love)
                RevelationSection("事业", result.career)
                RevelationSection("健康", result.health)

                Spacer(Modifier.height(12.dp))

                // 口诀区域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScoreBtnBg)
                        .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Column {
                        Text(
                            text = "📜 口诀",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result.verse,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = BlueLight,
                            lineHeight = 21.sp,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 再来一次按钮
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFb8860b),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.height(44.dp),
                ) {
                    Text("再来一次", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RevelationSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            text = "▸ $title",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Gold.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
    }
}
