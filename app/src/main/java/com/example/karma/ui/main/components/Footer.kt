package com.example.karma.ui.main.components

import androidx.compose.foundation.background
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

@Composable
fun Footer(
    confirmEnabled: Boolean,
    prayerEnabled: Boolean,
    onConfirm: () -> Unit,
    onPrayer: () -> Unit,
    onDivination: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A)),
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
            .background(Color(0xFF333333)),
    )
}
