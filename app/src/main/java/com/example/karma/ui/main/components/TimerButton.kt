package com.example.karma.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerButton(
    enabled: Boolean,
    selectedScore: Float?,
    selectedEvent: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreText = selectedScore?.let {
        if (it >= 0) "+${it}" else "${it}"
    } ?: ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (enabled) Color(0xFF1a3a5c)
                else Color(0xFF16213e)
            )
            .clickable(enabled = enabled) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (enabled && selectedScore != null && selectedEvent != null) {
            Text(
                text = "▶ 开始计时",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF69f0ae),
            )
            Text(
                text = "  |  $scoreText 分 · ${selectedEvent.take(8)}${if (selectedEvent.length > 8) "…" else ""}",
                fontSize = 12.sp,
                color = Color(0xFFaaaaaa),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = "请选择分数和事件后开始计时",
                fontSize = 13.sp,
                color = Color(0xFF555555),
            )
        }
    }
}
