package com.example.karma.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.example.karma.data.model.Rank

@Composable
fun Header(
    totalScore: Float,
    rank: Rank?,
    ranks: List<Rank> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
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
            )

            if (rank != null) {
                Spacer(Modifier.width(10.dp))
                val totalRanks = ranks.size
                val textColor = if (totalRanks > 0 && rank.level <= totalRanks * 2 / 3)
                    Color.White else Color(0xFFffd700)
                val bgColor = Color(rank.colorHex).copy(alpha = 0.4f)
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
            } else if (totalScore < 0) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "—",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                )
            }
        }
    }
}
