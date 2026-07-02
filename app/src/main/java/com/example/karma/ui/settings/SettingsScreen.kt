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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.util.LuckAmplifier
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
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    letterSpacing = 2.sp,
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
            RankSettingsCard(draft = draft, viewModel = viewModel)
            EventSettingsCard(draft = draft, viewModel = viewModel)
            HistorySettingsCard(draft = draft, viewModel = viewModel)
            DecaySettingsCard(draft = draft, viewModel = viewModel)
            LuckSettingsCard(draft = draft, viewModel = viewModel)
            BackgroundGradientCard(draft = draft, viewModel = viewModel)
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
            var scoreAxisRangeMinText by remember(draft.scoreAxisRangeMin) { mutableStateOf(formatFloat(draft.scoreAxisRangeMin)) }
            OutlinedTextField(
                value = scoreAxisRangeMinText,
                onValueChange = { v ->
                    scoreAxisRangeMinText = v
                    v.toFloatOrNull()?.let { viewModel.updateScoreAxisRangeMin(it) }
                },
                modifier = Modifier.widthIn(min = 70.dp),
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
            var scoreAxisRangeMaxText by remember(draft.scoreAxisRangeMax) { mutableStateOf(formatFloat(draft.scoreAxisRangeMax)) }
            OutlinedTextField(
                value = scoreAxisRangeMaxText,
                onValueChange = { v ->
                    scoreAxisRangeMaxText = v
                    v.toFloatOrNull()?.let { viewModel.updateScoreAxisRangeMax(it) }
                },
                modifier = Modifier.widthIn(min = 70.dp),
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
    var showGuideLineColorPicker by remember { mutableStateOf(false) }

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

        // 7. 指引线 — 粗细 + 颜色
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
                .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
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

        // 上次扣除日期
        Text(
            text = "上次扣除：${draft.lastDecayDate.ifEmpty { "尚未扣除" }}",
            fontSize = 12.sp,
            color = TextMuted,
        )
    }
}

// ============================================================
// LuckSettingsCard — 运气增幅
// ============================================================

@Composable
private fun LuckSettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    SettingsCard("运气增幅") {
        // 总开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("启用运气增幅", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Switch(
                checked = draft.luckEnabled,
                onCheckedChange = { viewModel.updateLuckEnabled(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Gold.copy(alpha = 0.3f),
                ),
            )
        }

        if (draft.luckEnabled) {
            Spacer(Modifier.height(12.dp))

            // T — 天数（多少天后只通过总和影响）
            Text("你觉得多少天以后的行为只能通过总和影响？", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            var luckTText by remember(draft.luckT) { mutableStateOf(draft.luckT.toInt().toString()) }
            OutlinedTextField(
                value = luckTText,
                onValueChange = { v ->
                    luckTText = v
                    v.toFloatOrNull()?.let {
                        if (it >= 1f && it <= 365f) viewModel.updateLuckT(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(12.dp))

            // b — 普通好事分值
            Text("你觉得做一件普通的好事值多少分？", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            var luckBText by remember(draft.luckB) { mutableStateOf(formatFloat(draft.luckB)) }
            OutlinedTextField(
                value = luckBText,
                onValueChange = { v ->
                    luckBText = v
                    v.toFloatOrNull()?.let {
                        if (it >= 0.1f && it <= 100f) viewModel.updateLuckB(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(12.dp))

            // W — 对应总分
            Text("你觉得最近一天下降 10 分，对应总的多少分？", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            var luckWText by remember(draft.luckW) { mutableStateOf(formatFloat(draft.luckW)) }
            OutlinedTextField(
                value = luckWText,
                onValueChange = { v ->
                    luckWText = v
                    v.toFloatOrNull()?.let {
                        if (it >= 1f && it <= 1000f) viewModel.updateLuckW(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
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

            // 导出参数 a、c（只读）
            Spacer(Modifier.height(12.dp))
            val aVal = 9.0 / (2.0 * draft.luckT * draft.luckT)
            val cIntegral = LuckAmplifier.integralExpMinusAt2(aVal, 0.0, 1.0)
            val cVal = draft.luckW.toDouble() / (10.0 * cIntegral)
            Text(
                "a = ${String.format("%.6f", aVal)}    c = ${String.format("%.4f", cVal)}",
                fontSize = 11.sp,
                color = TextMuted,
            )
            Text(
                "运气 = (总业力 + c × 近期波动积分) ÷ b",
                fontSize = 11.sp,
                color = TextMuted,
            )
        }
    }
}

// ============================================================
// BackgroundGradientCard — 背景渐变设置
// ============================================================

@Composable
private fun BackgroundGradientCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    var showBaseColorPicker by remember { mutableStateOf(false) }
    var showAccentColorPicker by remember { mutableStateOf(false) }

    SettingsCard("背景渐变设置") {
        // 起始色（顶部）
        Text("渐变起始色（顶部）", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(
            color = draft.themeGradientBaseColor,
            onClick = { showBaseColorPicker = true },
        )

        Spacer(Modifier.height(12.dp))

        // 结束色（底部）
        Text("渐变结束色（底部）", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(
            color = draft.themeGradientAccentColor,
            onClick = { showAccentColorPicker = true },
        )

        // 渐变预览条
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(draft.themeGradientBaseColor),
                            Color(draft.themeGradientAccentColor),
                        ),
                    )
                )
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
        )
    }

    // 起始色选择器
    if (showBaseColorPicker) {
        ColorPickerDialog(
            currentColor = draft.themeGradientBaseColor,
            onColorSelected = {
                viewModel.updateThemeGradientBaseColor(it)
                showBaseColorPicker = false
            },
            onDismiss = { showBaseColorPicker = false },
        )
    }

    // 氛围色选择器
    if (showAccentColorPicker) {
        ColorPickerDialog(
            currentColor = draft.themeGradientAccentColor,
            onColorSelected = {
                viewModel.updateThemeGradientAccentColor(it)
                showAccentColorPicker = false
            },
            onDismiss = { showAccentColorPicker = false },
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
            containerColor = Color(0xFF1A1A1A),
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
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
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

// ============================================================
// RankSettingsCard — 阶位设置（可变数量）
// ============================================================

@Composable
private fun RankSettingsCard(
    draft: com.example.karma.data.local.entity.KarmaSettingsEntity,
    viewModel: SettingsViewModel,
) {
    val deleteMode by viewModel.deleteMode.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf(0) }

    SettingsCard("阶位设置") {
        val count = draft.rankNames.size

        for (i in 0 until count) {
            val isLast = i == count - 1
            val name = draft.rankNames.getOrElse(i) { "?" }
            val color = draft.rankColors.getOrElse(i) { 0xFFFFFFFF }
            val threshold = draft.rankThresholds.getOrElse(i) { 0f }
            val decay = draft.rankDecayAmounts.getOrElse(i) { 2f }

            // 每个阶位卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .border(1.dp, Gold.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column {
                    // 第一行：名称 + 删除按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.updateRankName(i, it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = Gold,
                            ),
                            modifier = Modifier.weight(1f),
                        )

                        // 删除按钮（仅 deleteMode 显示）
                        if (deleteMode) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFcc0000))
                                    .clickable { viewModel.deleteRank(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "×", color = Color.White,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // 第二行：颜色 + 上限
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorSwatch(color = color) {
                            colorPickerTarget = i
                            showColorPicker = true
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("上限：", fontSize = 12.sp, color = TextMuted)
                        if (isLast) {
                            Text("∞", fontSize = 14.sp, color = TextMuted)
                        } else {
                            var rankThresholdText by remember(threshold) { mutableStateOf(formatFloat(threshold)) }
                            OutlinedTextField(
                                value = rankThresholdText,
                                onValueChange = { v ->
                                    rankThresholdText = v
                                    v.toFloatOrNull()?.let { viewModel.updateRankThreshold(i, it) }
                                },
                                modifier = Modifier.widthIn(min = 52.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall,
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

                    Spacer(Modifier.height(4.dp))

                    // 第三行：业力衰减
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("业力衰减：", fontSize = 12.sp, color = TextMuted)
                        IconButton(
                            onClick = { viewModel.updateRankDecayAmount(i, decay - 1f) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Text("−", fontSize = 16.sp, color = TextSecondary)
                        }
                        Text(
                            decay.toInt().toString(),
                            fontSize = 14.sp, color = Gold,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(
                            onClick = { viewModel.updateRankDecayAmount(i, decay + 1f) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Text("+", fontSize = 16.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // 底部按钮：+ / -
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            // [+] 添加
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(1.dp, Color(0xFF334444), RoundedCornerShape(6.dp))
                    .clickable { viewModel.addRank() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("+", fontSize = 16.sp, color = Color(0xFFa0c4ff))
            }

            Spacer(Modifier.width(8.dp))

            // [-] 删除模式切换
            val delActive = deleteMode && count > 1
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (delActive) Color(0xFF4a90d9) else Color(0xFF1A1A1A)
                    )
                    .border(
                        1.dp,
                        if (delActive) Color(0xFF4a90d9) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable(enabled = count > 1) { viewModel.toggleDeleteMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "−", fontSize = 16.sp,
                    color = when {
                        delActive -> Color.White
                        count <= 1 -> Color(0xFF666666)
                        else -> Color(0xFFa0c4ff)
                    },
                )
            }
        }
    }

    // 颜色选择器
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = draft.rankColors.getOrElse(colorPickerTarget) { 0xFFFFFFFF },
            onColorSelected = {
                viewModel.updateRankColor(colorPickerTarget, it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }
}

// ============================================================
// ColorPickerDialog — HSV 滑块取色器
// ============================================================

@Composable
private fun ColorPickerDialog(
    currentColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    // Long(0xAARRGGBB) → HSV
    val initialHsv = remember {
        val r = ((currentColor shr 16) and 0xFF).toInt()
        val g = ((currentColor shr 8) and 0xFF).toInt()
        val b = (currentColor and 0xFF).toInt()
        FloatArray(3).also { android.graphics.Color.RGBToHSV(r, g, b, it) }
    }

    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }

    // HSV → Long(0xAARRGGBB)
    val longColor by remember(hue, saturation, value) {
        derivedStateOf {
            val intColor = android.graphics.Color.HSVToColor(
                floatArrayOf(hue, saturation, value),
            )
            intColor.toLong() and 0xFFFFFFFFL
        }
    }

    // 从 HSV 导出 R/G/B 整数值
    val currentR by remember(hue, saturation, value) {
        derivedStateOf {
            val c = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
            (c shr 16) and 0xFF
        }
    }
    val currentG by remember(hue, saturation, value) {
        derivedStateOf {
            val c = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
            (c shr 8) and 0xFF
        }
    }
    val currentB by remember(hue, saturation, value) {
        derivedStateOf {
            val c = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
            c and 0xFF
        }
    }

    // RGB → HSV 转换回调
    val onRChange: (Int) -> Unit = { r ->
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(r, currentG, currentB, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    val onGChange: (Int) -> Unit = { g ->
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(currentR, g, currentB, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    val onBChange: (Int) -> Unit = { b ->
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(currentR, currentG, b, hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
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
                        .background(Color(longColor))
                        .border(2.dp, BorderSubtle, RoundedCornerShape(8.dp)),
                )

                Spacer(Modifier.height(16.dp))

                // 色相滑块
                HueSliderComponent(hue = hue, onHueChange = { hue = it })
                Spacer(Modifier.height(12.dp))

                // 饱和度滑块
                SaturationSliderComponent(
                    saturation = saturation,
                    hue = hue,
                    value = value,
                    onSaturationChange = { saturation = it },
                )
                Spacer(Modifier.height(12.dp))

                // 明度滑块
                ValueSliderComponent(
                    value = value,
                    hue = hue,
                    saturation = saturation,
                    onValueChange = { value = it },
                )

                // 分隔线 + R/G/B 数字输入
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderSubtle.copy(alpha = 0.3f)),
                )
                Spacer(Modifier.height(8.dp))

                RGBTextField(label = "R", currentValue = currentR, onValueChange = onRChange)
                Spacer(Modifier.height(4.dp))
                RGBTextField(label = "G", currentValue = currentG, onValueChange = onGChange)
                Spacer(Modifier.height(4.dp))
                RGBTextField(label = "B", currentValue = currentB, onValueChange = onBChange)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(longColor)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSecondary) }
        },
        containerColor = Color(0xFF1A1A1A),
    )
}

@Composable
private fun HueSliderComponent(
    hue: Float,
    onHueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("色相", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("${hue.toInt()}°", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Spacer(Modifier.height(2.dp))
        // 彩虹渐变色参考条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF0000), // 0° 红
                            Color(0xFFFFFF00), // 60° 黄
                            Color(0xFF00FF00), // 120° 绿
                            Color(0xFF00FFFF), // 180° 青
                            Color(0xFF0000FF), // 240° 蓝
                            Color(0xFFFF00FF), // 300° 紫
                            Color(0xFFFF0000), // 360° 红
                        ),
                    ),
                ),
        )
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = BorderSubtle,
            ),
        )
    }
}

@Composable
private fun SaturationSliderComponent(
    saturation: Float,
    hue: Float,
    value: Float,
    onSaturationChange: (Float) -> Unit,
) {
    val gradientColors = remember(hue, value) {
        val gray = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))
        val full = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value))
        listOf(
            Color(gray.toLong() and 0xFFFFFFFFL),
            Color(full.toLong() and 0xFFFFFFFFL),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("饱和度", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("${(saturation * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(gradientColors)),
        )
        Slider(
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = BorderSubtle,
            ),
        )
    }
}

@Composable
private fun ValueSliderComponent(
    value: Float,
    hue: Float,
    saturation: Float,
    onValueChange: (Float) -> Unit,
) {
    val gradientColors = remember(hue, saturation) {
        val full = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))
        listOf(
            Color.Black,
            Color(full.toLong() and 0xFFFFFFFFL),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("明度", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(gradientColors)),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
                inactiveTrackColor = BorderSubtle,
            ),
        )
    }
}

// ============================================================
// RGBTextField — 允许空值的数字输入框
// ============================================================

@Composable
private fun RGBTextField(
    label: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(currentValue.toString()) }
    val isFocused = remember { mutableStateOf(false) }

    // 滑块拖动时同步到文本（不覆盖用户正在编辑的字段）
    LaunchedEffect(currentValue) {
        if (!isFocused.value) {
            text = currentValue.toString()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            val filtered = newText.filter { it.isDigit() }
            text = filtered
            if (filtered.isNotEmpty()) {
                filtered.toIntOrNull()?.let { v ->
                    if (v in 0..255) onValueChange(v)
                }
            }
        },
        label = { Text(label, color = TextMuted) },
        modifier = modifier
            .widthIn(min = 56.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                if (!focusState.isFocused && text.isEmpty()) {
                    text = currentValue.toString()
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
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
        containerColor = Color(0xFF1A1A1A),
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
