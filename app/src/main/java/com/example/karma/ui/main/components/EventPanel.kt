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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun EventPanel(
    goodDeedPresets: List<String>,
    badDeedPresets: List<String>,
    goodResultPresets: List<String>,
    selectedEvent: String?,
    onEventSelected: (String) -> Unit,
    onCustomGoodDeedChanged: (String) -> Unit,
    onCustomBadDeedChanged: (String) -> Unit,
    onCustomGoodResultChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

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

        Spacer(Modifier.height(6.dp))

        // Scrollable event sections (with custom inputs inside)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // — 善业 —
            EventSection(
                title = "善业",
                titleColor = Color(0xFF69f0ae),
                events = goodDeedPresets,
                selectedEvent = selectedEvent,
                onEventSelected = onEventSelected,
                customPlaceholder = "自定义善业...",
                onCustomChanged = onCustomGoodDeedChanged,
            )

            // — 恶业 —
            EventSection(
                title = "恶业",
                titleColor = Color(0xFFff5252),
                events = badDeedPresets,
                selectedEvent = selectedEvent,
                onEventSelected = onEventSelected,
                customPlaceholder = "自定义恶业...",
                onCustomChanged = onCustomBadDeedChanged,
            )

            // — 善果 —
            EventSection(
                title = "善果",
                titleColor = Color(0xFFffd700),
                events = goodResultPresets.ifEmpty { listOf("（暂无预设事件）") },
                selectedEvent = selectedEvent,
                onEventSelected = onEventSelected,
                customPlaceholder = "自定义善果...",
                onCustomChanged = onCustomGoodResultChanged,
            )
        }
    }
}

@Composable
private fun EventSection(
    title: String,
    titleColor: Color,
    events: List<String>,
    selectedEvent: String?,
    onEventSelected: (String) -> Unit,
    customPlaceholder: String,
    onCustomChanged: (String) -> Unit,
) {
    var customText by remember { mutableStateOf("") }

    Column {
        // Section title
        Text(
            text = "── $title ──",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(vertical = 2.dp),
        )

        // Section items
        events.forEach { event ->
            val isSelected = selectedEvent == event
            val canSelect = !event.startsWith("（")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color(0xFFffd700), RoundedCornerShape(7.dp))
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        when {
                            isSelected -> Color(0xFFffd700).copy(alpha = 0.1f)
                            title == "善业" -> Color(0xFF69f0ae).copy(alpha = 0.08f)
                            title == "恶业" -> Color(0xFFff5252).copy(alpha = 0.08f)
                            else -> ScoreBtnBg
                        }
                    )
                    .padding(horizontal = 8.dp)
                    .defaultMinSize(minHeight = 13.dp)
                    .fillMaxWidth()
                    .then(
                        if (canSelect) {
                            Modifier.clickable { onEventSelected(event) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = event,
                    fontSize = 12.sp,
                    color = when {
                        isSelected -> Color(0xFFffd700)
                        !canSelect -> Color(0xFF666666)
                        else -> Color(0xFFa0c4ff)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Custom event input for this category — slightly taller than event items
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = customText,
            onValueChange = {
                customText = it
                onCustomChanged(it)
            },
            placeholder = { Text(customPlaceholder, fontSize = 11.sp, color = Color(0xFF666666)) },
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = titleColor.copy(alpha = 0.8f),
                fontSize = 12.sp,
            ),
            singleLine = false,
            minLines = 1,
            shape = RoundedCornerShape(6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = titleColor.copy(alpha = 0.5f),
                unfocusedBorderColor = BorderSubtle,
                cursorColor = titleColor,
                focusedContainerColor = ScoreBtnBg,
                unfocusedContainerColor = ScoreBtnBg,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 16.dp),
        )
    }
}
