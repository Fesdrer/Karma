package com.example.karma.ui.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.timer.TimerService
import com.example.karma.ui.timer.TimerStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun EventPanel(
    goodDeedPresets: List<String>,
    badDeedPresets: List<String>,
    goodResultPresets: List<String>,
    eventFlow: StateFlow<String?>,
    onEventSelected: (String) -> Unit,
    onCustomGoodDeedChanged: (String) -> Unit,
    onCustomBadDeedChanged: (String) -> Unit,
    onCustomGoodResultChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    // 计时控制
    timerEnabled: Boolean = false,
    selectedScore: Float? = null,
    onStopTimer: (elapsedMs: Long) -> Unit = {},
) {
    val selectedEvent by eventFlow.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
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

        // 计时控制（始终 2 倍高度）
        Spacer(Modifier.height(6.dp))
        val timerState by TimerService.timerState.collectAsState()
        val context = LocalContext.current
        val scoreText = selectedScore?.let {
            if (it >= 0) "+${it}" else "${it}"
        } ?: ""
        var displayMs by remember { mutableStateOf(timerState.currentElapsedMs()) }
        LaunchedEffect(timerState.status) {
            displayMs = timerState.currentElapsedMs()
            while (timerState.status == TimerStatus.RUNNING) {
                delay(200)
                displayMs = timerState.currentElapsedMs()
            }
        }

        when (timerState.status) {
            TimerStatus.IDLE, TimerStatus.STOPPED -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (timerEnabled) Color(0xFF1A1A1A)
                            else Color(0xFF111122)
                        )
                        .clickable(enabled = timerEnabled) {
                            TimerService.start(context, selectedScore ?: 0f, selectedEvent ?: "")
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (timerEnabled) {
                        Text(
                            text = "▶ 开始计时  $scoreText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF69f0ae),
                        )
                    } else {
                        Text(
                            text = "选择分数与事件",
                            fontSize = 11.sp,
                            color = Color(0xFF555555),
                        )
                    }
                }
            }
            TimerStatus.RUNNING, TimerStatus.PAUSED -> {
                val isRunning = timerState.status == TimerStatus.RUNNING
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(vertical = 6.dp),
                ) {
                    // 上半：时间
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = TimerService.formatTime(displayMs),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            textAlign = TextAlign.Center,
                        )
                    }
                    // 下半：固定大小图标按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 暂停/继续
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFb8860b))
                                .clickable {
                                    if (isRunning) TimerService.pause(context)
                                    else TimerService.resume(context)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isRunning) PauseIcon() else PlayIcon()
                        }
                        // 停止
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF333333))
                                .clickable { onStopTimer(displayMs) },
                            contentAlignment = Alignment.Center,
                        ) {
                            StopIcon()
                        }
                    }
                }
            }
        }
    }
}

// ===== 图标组件 =====

@Composable
private fun PauseIcon() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(Modifier.width(3.dp).height(14.dp).background(Color.White, RoundedCornerShape(1.dp)))
        Box(Modifier.width(3.dp).height(14.dp).background(Color.White, RoundedCornerShape(1.dp)))
    }
}

@Composable
private fun PlayIcon() {
    Canvas(Modifier.size(14.dp, 16.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, Color.White)
    }
}

@Composable
private fun StopIcon() {
    Box(Modifier.size(14.dp).background(Color.White, RoundedCornerShape(2.dp)))
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
                            title == "善果" -> Color(0xFFffd700).copy(alpha = 0.08f)
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

        // Custom event input — compact, ~1.2x event item height
        val fieldTextStyle = TextStyle(
            fontFamily = FontFamily.Serif,
            color = titleColor.copy(alpha = 0.8f),
            fontSize = 12.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ScoreBtnBg)
                .border(1.dp, titleColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            BasicTextField(
                value = customText,
                onValueChange = {
                    customText = it
                    onCustomChanged(it)
                },
                textStyle = fieldTextStyle,
                singleLine = false,
                cursorBrush = SolidColor(titleColor),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (customText.isEmpty()) {
                            Text(
                                text = customPlaceholder,
                                style = fieldTextStyle.copy(color = Color(0xFF666666)),
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
