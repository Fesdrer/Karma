package com.example.karma.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.theme.BorderSubtle
import kotlin.math.abs
import kotlinx.coroutines.launch
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.PanelBg
import com.example.karma.ui.theme.TextMuted
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    appContainer: AppContainer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(appContainer.repository)
    )
    val draft by viewModel.draft.collectAsState()
    val original by viewModel.original.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = Color(0xFF1a1a2e),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16213e))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("← 返回", color = Gold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                // 占位使标题居中
                Spacer(Modifier.width(64.dp))
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16213e))
                    .padding(16.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.save()
                        onBack()
                    },
                    enabled = draft != original,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF333333),
                        disabledContentColor = Color(0xFF666666),
                    ),
                ) {
                    Text("保存设置", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScoreSettingsCard(draft = draft, viewModel = viewModel)
            AxisSettingsCard(draft = draft, viewModel = viewModel)
            EventSettingsCard(draft = draft, viewModel = viewModel)
            HistorySettingsCard(draft = draft, viewModel = viewModel)
            DecaySettingsCard(draft = draft, viewModel = viewModel)
            ResetCard(viewModel = viewModel)
        }
    }
}

// ============================================================
// ScoreSettingsCard — 左边分数区域
// ============================================================

@Composable
private fun ScoreSettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    SettingsCard("左边分数区域") {
        // 字体大小 Slider 12~36, step=1
        SettingsSlider(
            label = "字体大小",
            value = draft.scoreAxisFontSize,
            valueRange = 12f..36f,
            steps = 23,
            onValueChange = { viewModel.updateScoreAxisFontSize(it) },
        )

        Spacer(Modifier.height(12.dp))

        // 显示范围：两个独立的数字输入框（不再强制对称）
        Text("显示范围", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = formatFloat(draft.scoreAxisRangeMin),
                onValueChange = { v ->
                    v.toFloatOrNull()?.let { viewModel.updateScoreAxisRangeMin(it) }
                },
                modifier = Modifier.width(70.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Text("  ~  ", color = TextMuted)
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = formatFloat(draft.scoreAxisRangeMax),
                onValueChange = { v ->
                    v.toFloatOrNull()?.let { viewModel.updateScoreAxisRangeMax(it) }
                },
                modifier = Modifier.width(70.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
            )
        }
    }
}

// ============================================================
// AxisSettingsCard — 中间刻度区域
// ============================================================

@Composable
private fun AxisSettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    var showLabelColorPicker by remember { mutableStateOf(false) }
    var showRankColorPicker by remember { mutableStateOf(false) }
    var showGuideLineColorPicker by remember { mutableStateOf(false) }
    var colorPickerTargetIndex by remember { mutableStateOf(0) }

    SettingsCard("中间刻度区域") {
        // 1. 刻度颜色
        Text("刻度颜色", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(
            color = draft.axisLabelColor,
            onClick = { showLabelColorPicker = true },
        )

        Spacer(Modifier.height(12.dp))

        // 2. 刻度粗细
        SettingsSlider("刻度粗细", draft.axisTickThickness, 0.5f..12.0f, 22, viewModel::updateAxisTickThickness)

        // 3. 字体大小
        SettingsSlider("字体大小", draft.axisLabelFontSize, 12f..56f, 43, viewModel::updateAxisLabelFontSize)

        // 4. 显示区间
        SettingsSlider("显示区间", draft.axisDisplayRange, 50f..500f, 44, viewModel::updateAxisDisplayRange)

        // 5. 近邻刻度 — 开关 + 范围
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("近邻刻度", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Switch(
                checked = draft.showNearbyTicks,
                onCheckedChange = { viewModel.updateShowNearbyTicks(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Gold.copy(alpha = 0.3f),
                ),
            )
        }
        if (draft.showNearbyTicks) {
            SettingsSlider("近邻范围", draft.nearbyTickRange, 5f..50f, 44, viewModel::updateNearbyTickRange)
        }

        // 6. 疏密程度
        Spacer(Modifier.height(8.dp))
        Text("疏密程度", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            "上方¼处 = +${draft.axisQuarterValue.toInt()} 分",
            fontSize = 12.sp,
            color = TextMuted,
        )
        Slider(
            value = draft.axisQuarterValue,
            onValueChange = { viewModel.updateAxisQuarterValue(it) },
            valueRange = 2f..50f,
            steps = 47,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("较稀疏", fontSize = 11.sp, color = TextMuted)
            Text("较密集", fontSize = 11.sp, color = TextMuted)
        }

        // 7. 阶位颜色 — 9个色块
        Spacer(Modifier.height(12.dp))
        Text("阶位颜色（点击修改）", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        val rankNames = listOf("壹阶", "贰阶", "叁阶", "肆阶", "伍阶", "陆阶", "柒阶", "捌阶", "玖阶")
        // 3行×3列，每个色块下方显示阶位名称
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    if (idx < draft.rankColors.size) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ColorSwatch(
                                color = draft.rankColors[idx],
                                onClick = {
                                    colorPickerTargetIndex = idx
                                    showRankColorPicker = true
                                },
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                rankNames[idx],
                                fontSize = 9.sp,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
        }
        // 8. 指引线 — 粗细 + 颜色
        Spacer(Modifier.height(12.dp))
        SettingsSlider("指引线粗细", draft.guideLineWidth, 0.5f..12f, 22, viewModel::updateGuideLineWidth)

        Spacer(Modifier.height(8.dp))
        Text("指引线颜色", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(
            color = draft.guideLineColor,
            onClick = { showGuideLineColorPicker = true },
        )
    }

    // 刻度颜色选择器
    if (showLabelColorPicker) {
        ColorPickerDialog(
            currentColor = draft.axisLabelColor,
            onColorSelected = { viewModel.updateAxisLabelColor(it) },
            onDismiss = { showLabelColorPicker = false },
        )
    }

    // 阶位颜色选择器
    if (showRankColorPicker) {
        ColorPickerDialog(
            currentColor = draft.rankColors.getOrElse(colorPickerTargetIndex) { 0xFFFFFFFFL },
            onColorSelected = { viewModel.updateRankColor(colorPickerTargetIndex, it) },
            onDismiss = { showRankColorPicker = false },
        )
    }

    // 指引线颜色选择器
    if (showGuideLineColorPicker) {
        ColorPickerDialog(
            currentColor = draft.guideLineColor,
            onColorSelected = { viewModel.updateGuideLineColor(it) },
            onDismiss = { showGuideLineColorPicker = false },
        )
    }
}

// ============================================================
// EventSettingsCard — 右边事件列表
// ============================================================

@Composable
private fun EventSettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    SettingsCard("右边事件列表") {
        Text(
            "每行一个事件，直接编辑即可增删",
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        // — 善业 —
        EventSection(
            title = "善业",
            titleColor = Color(0xFF69f0ae),
            events = draft.goodDeedPresets,
            onUpdate = { viewModel.updateGoodDeedPresets(it) },
        )

        Spacer(Modifier.height(12.dp))

        // — 恶业 —
        EventSection(
            title = "恶业",
            titleColor = Color(0xFFff5252),
            events = draft.badDeedPresets,
            onUpdate = { viewModel.updateBadDeedPresets(it) },
        )

        Spacer(Modifier.height(12.dp))

        // — 善果 —
        EventSection(
            title = "善果",
            titleColor = Color(0xFFffd700),
            events = draft.goodResultPresets,
            onUpdate = { viewModel.updateGoodResultPresets(it) },
        )
    }
}

@Composable
private fun EventSection(
    title: String,
    titleColor: Color,
    events: List<String>,
    onUpdate: (String) -> Unit,
) {
    var text by remember(events) {
        mutableStateOf(events.joinToString("\n"))
    }

    Column {
        Text(
            text = "■ $title",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                onUpdate(newText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            singleLine = false,
            minLines = 3,
            textStyle = MaterialTheme.typography.bodySmall,
            placeholder = { Text("输入${title}事件...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = titleColor,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = titleColor,
            ),
        )
    }
}

// ============================================================
// HistorySettingsCard — 历史记录设置
// ============================================================

@Composable
private fun HistorySettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    SettingsCard("历史记录设置") {
        SettingsSlider("线条粗细", draft.historyLineThickness, 0.5f..10f, 18, viewModel::updateHistoryLineThickness)
        SettingsSlider("点的半径", draft.historyDotRadius, 1f..16f, 29, viewModel::updateHistoryDotRadius)
    }
}

// ============================================================
// DecaySettingsCard — 业力衰减
// ============================================================

@Composable
private fun DecaySettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    SettingsCard("业力衰减") {
        // 总开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("启用衰减", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Switch(
                checked = draft.decayEnabled,
                onCheckedChange = { viewModel.updateDecayEnabled(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Gold.copy(alpha = 0.3f),
                ),
            )
        }

        Spacer(Modifier.height(8.dp))

        // 扣除时间 — 点击弹出选择对话框
        Text("扣除时间", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))

        var showTimePicker by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1a1a3e))
                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                .clickable { showTimePicker = true }
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = String.format("%02d:%02d", draft.decayHour, draft.decayMinute),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
        }

        if (showTimePicker) {
            TimePickerDialog(
                initialHour = draft.decayHour,
                initialMinute = draft.decayMinute,
                onConfirm = { hour, minute ->
                    viewModel.updateDecayTime(hour, minute)
                },
                onDismiss = { showTimePicker = false },
            )
        }

        Spacer(Modifier.height(12.dp))

        // 各阶位扣除量
        Text("各阶位扣除量：", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))

        for (row in 0..4) {
            Row {
                val idx1 = row * 2
                if (idx1 < 9) {
                    RankDecayItem(
                        rankIndex = idx1,
                        amount = draft.rankDecayAmounts.getOrElse(idx1) { 0f },
                        color = draft.rankColors.getOrElse(idx1) { 0xFF0055ffL },
                        onDecrement = { viewModel.updateRankDecayAmount(idx1, it - 1f) },
                        onIncrement = { viewModel.updateRankDecayAmount(idx1, it + 1f) },
                    )
                }
                Spacer(Modifier.width(12.dp))
                val idx2 = row * 2 + 1
                if (idx2 < 9) {
                    RankDecayItem(
                        rankIndex = idx2,
                        amount = draft.rankDecayAmounts.getOrElse(idx2) { 0f },
                        color = draft.rankColors.getOrElse(idx2) { 0xFF0055ffL },
                        onDecrement = { viewModel.updateRankDecayAmount(idx2, it - 1f) },
                        onIncrement = { viewModel.updateRankDecayAmount(idx2, it + 1f) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 上次扣除日期
        Text(
            text = "上次扣除：${draft.lastDecayDate.ifEmpty { "尚未扣除" }}",
            fontSize = 12.sp,
            color = TextMuted,
        )
    }
}

// ============================================================
// ResetCard — 重置默认
// ============================================================

@Composable
private fun ResetCard(viewModel: SettingsViewModel) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置", color = Gold) },
            text = { Text("所有设置将恢复为默认值，此操作不可撤销。", color = TextPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFff5252)),
                ) { Text("确定重置") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF16213e),
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFff5252),
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("重置所有设置为默认", fontSize = 14.sp)
                Text("（事件除外）", fontSize = 11.sp, color = Color(0xFFff5252).copy(alpha = 0.7f))
            }
        }
    }
}

// ============================================================
// Helper Composables
// ============================================================

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PanelBg)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Gold,
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SettingsSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
            ),
        )
        Text(
            formatFloat(value),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
            color = TextPrimary,
        )
    }
}

@Composable
private fun ColorSwatch(color: Long, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(color))
            .clickable { onClick() }
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
    )
}

@Composable
private fun RankDecayItem(
    rankIndex: Int,
    amount: Float,
    color: Long,
    onDecrement: (Float) -> Unit,
    onIncrement: (Float) -> Unit,
) {
    val rankNames = listOf("壹阶", "贰阶", "叁阶", "肆阶", "伍阶", "陆阶", "柒阶", "捌阶", "玖阶")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(color))
        )
        Spacer(Modifier.width(4.dp))
        Text(rankNames[rankIndex], fontSize = 13.sp, color = TextPrimary, modifier = Modifier.width(32.dp))
        IconButton(onClick = { onDecrement(amount) }, modifier = Modifier.size(28.dp)) {
            Text("−", fontSize = 16.sp, color = TextSecondary)
        }
        Text(
            amount.toInt().toString(),
            fontSize = 14.sp,
            color = Gold,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onIncrement(amount) }, modifier = Modifier.size(28.dp)) {
            Text("+", fontSize = 16.sp, color = TextSecondary)
        }
    }
}

// ============================================================
// ColorPickerDialog — RGB 输入 + 实时预览
// ============================================================

@Composable
private fun ColorPickerDialog(
    currentColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    // 从 currentColor (0xAARRGGBB) 提取 R/G/B
    val initialR = ((currentColor shr 16) and 0xFF).toInt()
    val initialG = ((currentColor shr 8) and 0xFF).toInt()
    val initialB = (currentColor and 0xFF).toInt()

    var r by remember { mutableStateOf(initialR.coerceIn(0, 255)) }
    var g by remember { mutableStateOf(initialG.coerceIn(0, 255)) }
    var b by remember { mutableStateOf(initialB.coerceIn(0, 255)) }

    val previewColor = remember(r, g, b) {
        0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色", color = Gold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 实时预览色块
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(previewColor))
                        .border(2.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                )

                Spacer(Modifier.height(16.dp))

                // R / G / B 三输入框
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorTextField("R", r, { r = it.coerceIn(0, 255) })
                    ColorTextField("G", g, { g = it.coerceIn(0, 255) })
                    ColorTextField("B", b, { b = it.coerceIn(0, 255) })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(previewColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF16213e),
    )
}

@Composable
private fun ColorTextField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            val filtered = newText.filter { it.isDigit() }
            text = filtered
            filtered.toIntOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label, color = TextMuted) },
        modifier = Modifier.width(70.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = BorderSubtle,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Gold,
        ),
    )
}

// ============================================================
// TimePickerDialog — 弹出式时间选择
// ============================================================

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择扣除时间", color = Gold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScrollPicker(
                    range = 0..23,
                    selected = selectedHour,
                    onSelected = { selectedHour = it },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    ":",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                ScrollPicker(
                    range = 0..59,
                    selected = selectedMinute,
                    onSelected = { selectedMinute = it },
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedHour, selectedMinute)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF16213e),
    )
}

// ============================================================
// ScrollPicker — 滑动选择器（修正：选中项居中）
// ============================================================

@Composable
private fun ScrollPicker(
    range: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseItems = range.toList()
    val baseSize = baseItems.size
    // 重复 3 份实现循环效果
    val items = remember(range) {
        buildList { repeat(3) { addAll(range.toList()) } }
    }
    val totalSize = items.size

    val itemHeight = 44.dp
    val visibleItems = 5
    val scope = rememberCoroutineScope()

    // 初始定位在中间副本（第 2 份），并让选中项居中
    val startIndex = baseSize + baseItems.indexOf(selected).coerceAtLeast(0) - visibleItems / 2
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex.coerceAtLeast(0)
    )

    // 用 layoutInfo 找到视口正中间的项
    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf 0
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                abs((info.offset + info.size / 2) - viewportCenter)
            }?.index?.coerceIn(0, totalSize - 1) ?: 0
        }
    }

    // 选中值通过取模归一化到 base 范围；边缘检测跳回中间副本
    LaunchedEffect(centerItemIndex) {
        onSelected(baseItems[centerItemIndex % baseSize])
        // 边缘检测：接近边界时跳回中间（无动画）
        if (centerItemIndex < baseSize) {
            listState.scrollToItem(centerItemIndex + baseSize)
        } else if (centerItemIndex >= baseSize * 2) {
            listState.scrollToItem(centerItemIndex - baseSize)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItems)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // 选中高亮条（始终保持在正中间）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(Gold.copy(alpha = 0.12f))
                .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(4.dp)),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { index, value ->
                val isCenter = index == centerItemIndex
                Text(
                    text = String.format("%02d", value),
                    fontSize = if (isCenter) 22.sp else 14.sp,
                    fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCenter) Color.White else TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            scope.launch { listState.animateScrollToItem(index) }
                            onSelected(baseItems[value % baseSize])
                        },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ============================================================
// Format Helper
// ============================================================

private fun formatFloat(v: Float): String {
    return if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)
}
