package com.example.karma.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun DivinationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var result by remember { mutableStateOf<Int?>(null) }
    var backHandled by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("气运测试", "大衍筮法", "小六壬")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Back button
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 32.dp)
                .clickable {
                    if (!backHandled) {
                        backHandled = true
                        onBack()
                    }
                },
        ) {
            Text(
                text = "← 返回",
                fontSize = 18.sp,
                color = Color(0xFFa0c4ff),
                modifier = Modifier.padding(12.dp),
            )
        }

        // Center content — varies by tab
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 72.dp),  // room for tab bar
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (selectedTab) {
                0 -> {
                    // 气运测试 — 数字偏上，按钮偏下
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = result?.toString() ?: "?",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFffd700),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            var cnt = 0
                            for (i in 1..1000) {
                                if (Random.nextInt(1, 1001) <= 490) {
                                    cnt++
                                }
                            }
                            result = cnt
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4a90d9),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = if (result == null) "开始" else "再来一次",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                1 -> {
                    // 大衍筮法 — 暂空白
                    Text(
                        text = "大衍筮法\n\n暂未开放",
                        fontSize = 20.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                    )
                }
                2 -> {
                    // 小六壬 — 暂空白
                    Text(
                        text = "小六壬\n\n暂未开放",
                        fontSize = 20.sp,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Bottom tab bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF16213e))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(0xFF4a90d9)
                            else Color(0xFF1a1a3e)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF4a90d9) else Color(0xFF334444),
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { selectedTab = index }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color(0xFFa0c4ff),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                if (index < tabs.size - 1) {
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}
