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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                    enabled = viewModel.hasChanges(),
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

        // 显示范围：两个数字输入框
        Text("显示范围", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = formatFloat(-draft.scoreAxisRange),
                onValueChange = { v ->
                    v.toFloatOrNull()?.let { viewModel.updateScoreAxisRange(kotlin.math.abs(it)) }
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
                value = formatFloat(draft.scoreAxisRange),
                onValueChange = { v ->
                    v.toFloatOrNull()?.let { viewModel.updateScoreAxisRange(it) }
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
        SettingsSlider("刻度粗细", draft.axisTickThickness, 0.5f..3.0f, 4, viewModel::updateAxisTickThickness)

        // 3. 字体大小
        SettingsSlider("字体大小", draft.axisLabelFontSize, 12f..32f, 19, viewModel::updateAxisLabelFontSize)

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
        // 3行×3列
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (col in 0 until 3) {
                    val idx = row * 3 + col
                    if (idx < draft.rankColors.size) {
                        ColorSwatch(
                            color = draft.rankColors[idx],
                            onClick = {
                                colorPickerTargetIndex = idx
                                showRankColorPicker = true
                            },
                        )
                    }
                }
            }
        }
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
        Spacer(Modifier.height(8.dp))

        var text by remember(draft.eventPresets) {
            mutableStateOf(draft.eventPresets.joinToString("\n"))
        }

        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                text = newText
                viewModel.updateEventPresets(newText)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            singleLine = false,
            minLines = 6,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text("帮助他人\n早起早睡\n锻炼身体\n...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Gold,
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
        SettingsSlider("线条粗细", draft.historyLineThickness, 0.5f..5f, 8, viewModel::updateHistoryLineThickness)
        SettingsSlider("点的半径", draft.historyDotRadius, 1f..8f, 13, viewModel::updateHistoryDotRadius)
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

        // 扣除时间
        Text("扣除时间：${draft.decayHour}:${String.format("%02d", draft.decayMinute)}",
            style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

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
            Text("重置所有设置为默认")
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
// ColorPickerDialog
// ============================================================

@Composable
private fun ColorPickerDialog(
    currentColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val presetColors = listOf(
        0xFFFFFFFFL,  // 白
        0xFFCCCCCCL,  // 浅灰
        0xFF666666L,  // 深灰
        0xFFFFD700L,  // 金
        0xFF4A90D9L,  // 蓝
        0xFFFF5252L,  // 红
        0xFF69F0AEL,  // 绿
        0xFFFF9800L,  // 橙
        0xFF00BCD4L,  // 青
        0xFF9C27B0L,  // 紫
        0xFFF48FB1L,  // 粉
        0xFF8D6E63L,  // 棕
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色", color = Gold) },
        text = {
            Column {
                // 当前选中的颜色预览
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(currentColor))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("当前选择", color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))

                // 色块网格 3×4
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in presetColors.chunked(4)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(color))
                                        .border(
                                            width = if (color == currentColor) 2.dp else 1.dp,
                                            color = if (color == currentColor) Color.White else BorderSubtle,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .clickable {
                                            onColorSelected(color)
                                            onDismiss()
                                        },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF16213e),
    )
}

// ============================================================
// Format Helper
// ============================================================

private fun formatFloat(v: Float): String {
    return if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)
}
