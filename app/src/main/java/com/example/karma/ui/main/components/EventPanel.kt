package com.example.karma.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun EventPanel(
    eventPresets: List<String>,
    selectedEvent: String?,
    onEventSelected: (String) -> Unit,
    onCustomEventChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
    ) {
        // Title
        Text(
            text = "事件",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFffd700),
        )

        // Event list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(eventPresets) { event ->
                val isSelected = selectedEvent == event
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    2.dp,
                                    Color(0xFFffd700),
                                    RoundedCornerShape(7.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            if (isSelected) Color(0xFFffd700).copy(alpha = 0.1f)
                            else ScoreBtnBg
                        )
                        .padding(horizontal = 8.dp)
                        .defaultMinSize(minHeight = 13.dp)
                        .fillMaxWidth()
                        .clickable { onEventSelected(event) },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = event,
                        fontSize = 12.sp,
                        color = if (isSelected) Color(0xFFffd700) else Color(0xFFa0c4ff),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Custom event input
        var customText by remember { mutableStateOf("") }
        OutlinedTextField(
            value = customText,
            onValueChange = {
                customText = it
                onCustomEventChanged(it)
            },
            placeholder = { Text("自定义事件...", fontSize = 14.sp, color = Color(0xFF666666)) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFffd700),
                unfocusedBorderColor = BorderSubtle,
                cursorColor = Color(0xFFffd700),
                focusedContainerColor = ScoreBtnBg,
                unfocusedContainerColor = ScoreBtnBg,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
