package com.example.karma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
fun ChartTooltip(
    point: com.example.karma.ui.history.AggregatedPoint?,
    modifier: Modifier = Modifier,
) {
    if (point == null) return

    val timeStr = dateFormat.format(Date(point.timestamp))
    val deltaStr = if (point.delta >= 0) "+%.1f".format(point.delta) else "%.1f".format(point.delta)

    Column(
        modifier = modifier
            .widthIn(max = 220.dp)
            .background(Color(0xFF151515), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF334444), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        TooltipRow("时间", timeStr, Color(0xFFe0e0e0))
        Spacer(Modifier.height(4.dp))
        TooltipRow("变动", deltaStr, if (point.delta >= 0) Color(0xFF69f0ae) else Color(0xFFff5252))
        Spacer(Modifier.height(4.dp))
        TooltipRow("事件", point.event.ifEmpty { "—" }, Color(0xFFe0e0e0))
        Spacer(Modifier.height(4.dp))
        TooltipRow("总分", "%.1f".format(point.totalAfter), Color(0xFFe0e0e0))
    }
}

@Composable
private fun TooltipRow(
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF888888),
            modifier = Modifier.width(40.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}
