package com.example.karma.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.Gold

@Composable
fun Footer(
    confirmEnabled: Boolean,
    prayerEnabled: Boolean,
    onConfirm: () -> Unit,
    onPrayer: () -> Unit,
    onBet: () -> Unit,
    onDivination: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 不加水平内缩：边框盒左右与上方分数栏左边/事件栏右边对齐
            .padding(vertical = 8.dp)
            .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 确认
        FooterSegment(
            text = "确认",
            enabled = confirmEnabled,
            activeColor = Color(0xFF4a90d9),
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 祈福
        FooterSegment(
            text = "祈福",
            enabled = prayerEnabled,
            activeColor = Color(0xFFb8860b),
            onClick = onPrayer,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 誓约
        FooterSegment(
            text = "誓约",
            enabled = true,
            activeColor = Color(0xFF7b68ee),   // 区别于祈福(金)、占卜(灰) 的紫色系
            onClick = onBet,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 占卜
        FooterSegment(
            text = "占卜",
            enabled = true,
            activeColor = Color(0xFF555555),
            onClick = onDivination,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 历史
        FooterSegment(
            text = "历史",
            enabled = true,
            activeColor = Color(0xFF555555),
            onClick = onHistory,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 设置
        FooterSegment(
            text = "⚙",
            enabled = true,
            activeColor = Color(0xFF555555),
            onClick = onSettings,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FooterSegment(
    text: String,
    enabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (enabled) activeColor.copy(alpha = 0.15f)
                else Color(0xFF222222)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> Color(0xFF555555)
                activeColor == Color(0xFF4a90d9) -> Color(0xFF4a90d9)
                activeColor == Color(0xFFb8860b) -> Color(0xFFb8860b)
                activeColor == Color(0xFF7b68ee) -> Color(0xFF7b68ee)
                else -> Color(0xFFa0c4ff)
            },
        )
    }
}

@Composable
private fun FooterDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(Color(0xFFb8860b).copy(alpha = 0.25f)),
    )
}
