package com.example.karma.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.data.local.entity.DailyMustDoDeed
import com.example.karma.di.AppContainer
import com.example.karma.util.LuckAmplifier
import com.example.karma.ui.theme.BorderSubtle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.PanelBg
import com.example.karma.ui.theme.TextMuted
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

// ============================================================
// 设置页分类定义（根页只显示分类入口，点进去看具体设置）
// ============================================================

private enum class SettingsCategory(val title: String, val subtitle: String) {
    Appearance("外观与图表", "分数轴、刻度、历史图表、背景渐变"),
    PositiveRanks("正阶位体系", "正阶的名称、颜色、上限、衰减与占卜次数"),
    NegativeRanks("负阶位体系", "负阶的名称、颜色、下限与占卜次数"),
    Events("事件管理", "善业、恶业、善果预设"),
    DailyMustDo("每日必做", "善业每日必做与未完成扣分"),
    Multipliers("乘法功能", "可用的乘法按钮及其数值"),
    Mechanics("业力机制", "业力衰减与运气增幅"),
    Splash("启动画面", "启动经文与停留时长"),
    General("重置", "选择性重置设置与数据"),
}

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
    val deleteMode by viewModel.deleteMode.collectAsState()
    val addMode by viewModel.addMode.collectAsState()
    val negativeAddMode by viewModel.negativeAddMode.collectAsState()
    val negativeDeleteMode by viewModel.negativeDeleteMode.collectAsState()
    // 事件增删/排序模式（v3.13）
    val goodDeedAddMode by viewModel.goodDeedAddMode.collectAsState()
    val goodDeedDeleteMode by viewModel.goodDeedDeleteMode.collectAsState()
    val badDeedAddMode by viewModel.badDeedAddMode.collectAsState()
    val badDeedDeleteMode by viewModel.badDeedDeleteMode.collectAsState()
    val goodResultAddMode by viewModel.goodResultAddMode.collectAsState()
    val goodResultDeleteMode by viewModel.goodResultDeleteMode.collectAsState()
    // 乘法按钮增删/排序模式（v4.0）
    val multiplierAddMode by viewModel.multiplierAddMode.collectAsState()
    val multiplierDeleteMode by viewModel.multiplierDeleteMode.collectAsState()
    // 选择性重置：勾选「历史记录」后保存按钮也要可点（历史不在草稿里，draft 可能没变化）
    val pendingClearHistory by viewModel.pendingClearHistory.collectAsState()

    // 当前打开的分类；null = 分类入口页
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    // 分类子页面按系统返回键先回到分类入口页，再按一次才退出设置
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = {
                    if (selectedCategory == null) onBack() else selectedCategory = null
                }) {
                    Text(if (selectedCategory == null) "← 返回" else "← 设置", color = Gold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = selectedCategory?.title ?: "设置",
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
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.save()
                        onBack()
                    },
                    enabled = draft != original || pendingClearHistory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF333333),
                        disabledContentColor = Color(0xFF666666),
                    ),
                ) {
                    Text("保存设置", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        // 分类间切换淡入淡出（与页面过渡一致）；滚动位置与局部编辑状态随内容重建而重置
        Crossfade(
            targetState = selectedCategory,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            modifier = Modifier.fillMaxSize(),
            label = "settingsCategory",
        ) { cat ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (cat == null) {
                    // 分类入口页：只显示各分类的进入按钮
                    SettingsCategory.entries.forEach { c ->
                        CategoryEntryCard(category = c, onClick = { selectedCategory = c })
                    }
                } else {
                    when (cat) {
                        SettingsCategory.Appearance -> {
                            ScoreSettingsCard(
                                scoreAxisFontSize = draft.scoreAxisFontSize,
                                scoreAxisRangeMin = draft.scoreAxisRangeMin,
                                scoreAxisRangeMax = draft.scoreAxisRangeMax,
                                onFontSizeChange = { viewModel.updateScoreAxisFontSize(it) },
                                onRangeMinChange = { viewModel.updateScoreAxisRangeMin(it) },
                                onRangeMaxChange = { viewModel.updateScoreAxisRangeMax(it) },
                            )
                            AxisSettingsCard(
                                axisLabelColor = draft.axisLabelColor,
                                axisTickThickness = draft.axisTickThickness,
                                axisLabelFontSize = draft.axisLabelFontSize,
                                axisDisplayRange = draft.axisDisplayRange,
                                showNearbyTicks = draft.showNearbyTicks,
                                nearbyTickRange = draft.nearbyTickRange,
                                axisQuarterValue = draft.axisQuarterValue,
                                dotColor = draft.dotColor,
                                onLabelColorChange = { viewModel.updateAxisLabelColor(it) },
                                onTickThicknessChange = { viewModel.updateAxisTickThickness(it) },
                                onLabelFontSizeChange = { viewModel.updateAxisLabelFontSize(it) },
                                onDisplayRangeChange = { viewModel.updateAxisDisplayRange(it) },
                                onShowNearbyChange = { viewModel.updateShowNearbyTicks(it) },
                                onNearbyRangeChange = { viewModel.updateNearbyTickRange(it) },
                                onQuarterValueChange = { viewModel.updateAxisQuarterValue(it) },
                                onDotColorChange = { viewModel.updateDotColor(it) },
                            )
                            HistorySettingsCard(
                                historyLineThickness = draft.historyLineThickness,
                                historyDotRadius = draft.historyDotRadius,
                                onLineThicknessChange = { viewModel.updateHistoryLineThickness(it) },
                                onDotRadiusChange = { viewModel.updateHistoryDotRadius(it) },
                            )
                            BackgroundGradientCard(
                                themeGradientBaseColor = draft.themeGradientBaseColor,
                                themeGradientAccentColor = draft.themeGradientAccentColor,
                                onBaseColorChange = { viewModel.updateThemeGradientBaseColor(it) },
                                onAccentColorChange = { viewModel.updateThemeGradientAccentColor(it) },
                            )
                        }
                        SettingsCategory.PositiveRanks -> {
                            PositiveRankSettingsCard(
                                rankNames = draft.rankNames,
                                rankColors = draft.rankColors,
                                rankThresholds = draft.rankThresholds,
                                rankDecayAmounts = draft.rankDecayAmounts,
                                rankDivinationLimits = draft.rankDivinationLimits,
                                deleteMode = deleteMode,
                                addMode = addMode,
                                onRankNameChange = { i, v -> viewModel.updateRankName(i, v) },
                                onRankColorChange = { i, v -> viewModel.updateRankColor(i, v) },
                                onRankThresholdChange = { i, v -> viewModel.updateRankThreshold(i, v) },
                                onRankDecayChange = { i, v -> viewModel.updateRankDecayAmount(i, v) },
                                onRankDivinationLimitChange = { i, v -> viewModel.updateRankDivinationLimit(i, v) },
                                onAddRankAfter = { index -> viewModel.addRankAfter(index) },
                                onAddRankAtTop = { viewModel.addRankAtTop() },
                                onDeleteRank = { viewModel.deleteRank(it) },
                                onToggleAddMode = { viewModel.toggleAddMode() },
                                onToggleDeleteMode = { viewModel.toggleDeleteMode() },
                            )
                        }
                        SettingsCategory.NegativeRanks -> {
                            NegativeRankSettingsCard(
                                negativeRankNames = draft.negativeRankNames,
                                negativeRankColors = draft.negativeRankColors,
                                negativeRankThresholds = draft.negativeRankThresholds,
                                negativeRankDivinationLimits = draft.negativeRankDivinationLimits,
                                negativeAddMode = negativeAddMode,
                                negativeDeleteMode = negativeDeleteMode,
                                onNegativeRankNameChange = { i, v -> viewModel.updateNegativeRankName(i, v) },
                                onNegativeRankColorChange = { i, v -> viewModel.updateNegativeRankColor(i, v) },
                                onNegativeRankThresholdChange = { i, v -> viewModel.updateNegativeRankThreshold(i, v) },
                                onNegativeRankDivinationLimitChange = { i, v -> viewModel.updateNegativeRankDivinationLimit(i, v) },
                                onAddNegativeRankAfter = { index -> viewModel.addNegativeRankAfter(index) },
                                onAddNegativeRankAtTop = { viewModel.addNegativeRankAtTop() },
                                onDeleteNegativeRank = { viewModel.deleteNegativeRank(it) },
                                onToggleNegativeAddMode = { viewModel.toggleNegativeAddMode() },
                                onToggleNegativeDeleteMode = { viewModel.toggleNegativeDeleteMode() },
                            )
                        }
                        SettingsCategory.Events -> {
                            EventSettingsCard(
                                goodDeed = EventListUi(
                                    title = "善业", titleColor = Color(0xFF69f0ae),
                                    presets = draft.goodDeedPresets,
                                    defaultScores = draft.goodDeedDefaultScores,
                                    addMode = goodDeedAddMode,
                                    deleteMode = goodDeedDeleteMode,
                                    onNameChange = { i, v -> viewModel.updateGoodDeedName(i, v) },
                                    onScoreChange = { i, v -> viewModel.updateGoodDeedDefaultScore(i, v) },
                                    onAddAfter = { viewModel.addGoodDeedAfter(it) },
                                    onAddAtTop = { viewModel.addGoodDeedAtTop() },
                                    onDelete = { viewModel.deleteGoodDeed(it) },
                                    onMove = { from, to -> viewModel.moveGoodDeed(from, to) },
                                    onToggleAddMode = { viewModel.toggleGoodDeedAddMode() },
                                    onToggleDeleteMode = { viewModel.toggleGoodDeedDeleteMode() },
                                ),
                                badDeed = EventListUi(
                                    title = "恶业", titleColor = Color(0xFFff5252),
                                    presets = draft.badDeedPresets,
                                    defaultScores = draft.badDeedDefaultScores,
                                    addMode = badDeedAddMode,
                                    deleteMode = badDeedDeleteMode,
                                    onNameChange = { i, v -> viewModel.updateBadDeedName(i, v) },
                                    onScoreChange = { i, v -> viewModel.updateBadDeedDefaultScore(i, v) },
                                    onAddAfter = { viewModel.addBadDeedAfter(it) },
                                    onAddAtTop = { viewModel.addBadDeedAtTop() },
                                    onDelete = { viewModel.deleteBadDeed(it) },
                                    onMove = { from, to -> viewModel.moveBadDeed(from, to) },
                                    onToggleAddMode = { viewModel.toggleBadDeedAddMode() },
                                    onToggleDeleteMode = { viewModel.toggleBadDeedDeleteMode() },
                                ),
                                goodResult = EventListUi(
                                    title = "善果", titleColor = Color(0xFFffd700),
                                    presets = draft.goodResultPresets,
                                    defaultScores = draft.goodResultDefaultScores,
                                    addMode = goodResultAddMode,
                                    deleteMode = goodResultDeleteMode,
                                    onNameChange = { i, v -> viewModel.updateGoodResultName(i, v) },
                                    onScoreChange = { i, v -> viewModel.updateGoodResultDefaultScore(i, v) },
                                    onAddAfter = { viewModel.addGoodResultAfter(it) },
                                    onAddAtTop = { viewModel.addGoodResultAtTop() },
                                    onDelete = { viewModel.deleteGoodResult(it) },
                                    onMove = { from, to -> viewModel.moveGoodResult(from, to) },
                                    onToggleAddMode = { viewModel.toggleGoodResultAddMode() },
                                    onToggleDeleteMode = { viewModel.toggleGoodResultDeleteMode() },
                                ),
                            )
                        }
                        SettingsCategory.DailyMustDo -> {
                            DailyMustDoCard(
                                goodDeedPresets = draft.goodDeedPresets,
                                dailyMustDoDeeds = draft.dailyMustDoDeeds,
                                onToggle = { name, enabled -> viewModel.toggleDailyMustDo(name, enabled) },
                                onPenaltyChange = { name, penalty -> viewModel.updateDailyMustDoPenalty(name, penalty) },
                            )
                        }
                        SettingsCategory.Multipliers -> {
                            MultiplierSettingsCard(
                                multipliers = draft.multiplierPresets,
                                addMode = multiplierAddMode,
                                deleteMode = multiplierDeleteMode,
                                onValueChange = { i, v -> viewModel.updateMultiplier(i, v) },
                                onAddAfter = { viewModel.addMultiplierAfter(it) },
                                onAddAtTop = { viewModel.addMultiplierAtTop() },
                                onDelete = { viewModel.deleteMultiplier(it) },
                                onMove = { from, to -> viewModel.moveMultiplier(from, to) },
                                onToggleAddMode = { viewModel.toggleMultiplierAddMode() },
                                onToggleDeleteMode = { viewModel.toggleMultiplierDeleteMode() },
                            )
                        }
                        SettingsCategory.Mechanics -> {
                            DecaySettingsCard(
                                decayEnabled = draft.decayEnabled,
                                decayHour = draft.decayHour,
                                decayMinute = draft.decayMinute,
                                lastDecayDate = draft.lastDecayDate,
                                rankDecayAmounts = draft.rankDecayAmounts,
                                rankThresholds = draft.rankThresholds,
                                rankNames = draft.rankNames,
                                onDecayEnabledChange = { viewModel.updateDecayEnabled(it) },
                                onDecayTimeChange = { h, m -> viewModel.updateDecayTime(h, m) },
                            )
                            LuckSettingsCard(
                                luckEnabled = draft.luckEnabled,
                                luckT = draft.luckT,
                                luckB = draft.luckB,
                                luckW = draft.luckW,
                                onLuckEnabledChange = { viewModel.updateLuckEnabled(it) },
                                onLuckTChange = { viewModel.updateLuckT(it) },
                                onLuckBChange = { viewModel.updateLuckB(it) },
                                onLuckWChange = { viewModel.updateLuckW(it) },
                            )
                        }
                        SettingsCategory.Splash -> {
                            SplashSettingsCard(
                                splashScripture = draft.splashScripture,
                                splashDurationSec = draft.splashDurationSec,
                                onScriptureChange = { viewModel.updateSplashScripture(it) },
                                onDurationChange = { viewModel.updateSplashDuration(it) },
                            )
                        }
                        SettingsCategory.General -> {
                            SelectiveResetCard(
                                onReset = { groups -> viewModel.resetGroups(groups) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// CategoryEntryCard — 分类入口卡片
// ============================================================

@Composable
private fun CategoryEntryCard(
    category: SettingsCategory,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = category.subtitle,
                fontSize = 12.sp,
                color = TextMuted,
            )
        }
        Text("›", fontSize = 22.sp, color = Gold)
    }
}

// ============================================================
// ScoreSettingsCard — 左边分数区域
// ============================================================

@Composable
private fun ScoreSettingsCard(
    scoreAxisFontSize: Float,
    scoreAxisRangeMin: Float,
    scoreAxisRangeMax: Float,
    onFontSizeChange: (Float) -> Unit,
    onRangeMinChange: (Float) -> Unit,
    onRangeMaxChange: (Float) -> Unit,
) {
    SettingsCard("左边分数区域") {
        SettingsSlider(
            label = "字体大小",
            value = scoreAxisFontSize,
            valueRange = 12f..36f,
            steps = 23,
            onValueChange = onFontSizeChange,
        )

        Spacer(Modifier.height(12.dp))

        Text("显示范围", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            var scoreAxisRangeMinText by remember(scoreAxisRangeMin) { mutableStateOf(formatFloat(scoreAxisRangeMin)) }
            OutlinedTextField(
                value = scoreAxisRangeMinText,
                onValueChange = { v ->
                    scoreAxisRangeMinText = v
                    v.toFloatOrNull()?.let { onRangeMinChange(it) }
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
            var scoreAxisRangeMaxText by remember(scoreAxisRangeMax) { mutableStateOf(formatFloat(scoreAxisRangeMax)) }
            OutlinedTextField(
                value = scoreAxisRangeMaxText,
                onValueChange = { v ->
                    scoreAxisRangeMaxText = v
                    v.toFloatOrNull()?.let { onRangeMaxChange(it) }
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
    axisLabelColor: Long,
    axisTickThickness: Float,
    axisLabelFontSize: Float,
    axisDisplayRange: Float,
    showNearbyTicks: Boolean,
    nearbyTickRange: Float,
    axisQuarterValue: Float,
    dotColor: Long,
    onLabelColorChange: (Long) -> Unit,
    onTickThicknessChange: (Float) -> Unit,
    onLabelFontSizeChange: (Float) -> Unit,
    onDisplayRangeChange: (Float) -> Unit,
    onShowNearbyChange: (Boolean) -> Unit,
    onNearbyRangeChange: (Float) -> Unit,
    onQuarterValueChange: (Float) -> Unit,
    onDotColorChange: (Long) -> Unit,
) {
    var showLabelColorPicker by remember { mutableStateOf(false) }
    var showDotColorPicker by remember { mutableStateOf(false) }

    SettingsCard("中间刻度区域") {
        Text("刻度颜色", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(color = axisLabelColor, onClick = { showLabelColorPicker = true })

        Spacer(Modifier.height(12.dp))

        SettingsSlider("刻度粗细", axisTickThickness, 0.5f..12.0f, 22, onTickThicknessChange)
        SettingsSlider("字体大小", axisLabelFontSize, 12f..56f, 43, onLabelFontSizeChange)
        SettingsSlider("显示区间", axisDisplayRange, 50f..500f, 44, onDisplayRangeChange)

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("近邻刻度", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Switch(
                checked = showNearbyTicks,
                onCheckedChange = onShowNearbyChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Gold.copy(alpha = 0.3f),
                ),
            )
        }
        if (showNearbyTicks) {
            SettingsSlider("近邻范围", nearbyTickRange, 5f..50f, 44, onNearbyRangeChange)
        }

        Spacer(Modifier.height(8.dp))
        Text("疏密程度", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text("上方¼处 = +${axisQuarterValue.toInt()} 分", fontSize = 12.sp, color = TextMuted)
        var localQuarterValue by remember { mutableFloatStateOf(axisQuarterValue) }
        LaunchedEffect(axisQuarterValue) {
            if (axisQuarterValue != localQuarterValue) localQuarterValue = axisQuarterValue
        }
        Slider(
            value = localQuarterValue,
            onValueChange = { localQuarterValue = it },
            onValueChangeFinished = { onQuarterValueChange(localQuarterValue) },
            valueRange = 2f..50f,
            steps = 47,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("较稀疏", fontSize = 11.sp, color = TextMuted)
            Text("较密集", fontSize = 11.sp, color = TextMuted)
        }

        Spacer(Modifier.height(12.dp))
        Text("光点颜色", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(color = dotColor, onClick = { showDotColorPicker = true })
    }

    if (showLabelColorPicker) {
        ColorPickerDialog(
            currentColor = axisLabelColor,
            onColorSelected = { onLabelColorChange(it) },
            onDismiss = { showLabelColorPicker = false },
        )
    }
    if (showDotColorPicker) {
        ColorPickerDialog(
            currentColor = dotColor,
            onColorSelected = { onDotColorChange(it) },
            onDismiss = { showDotColorPicker = false },
        )
    }
}

// ============================================================
// EventSettingsCard — 事件管理（v3.13 起：每条事件独立编辑默认分数 + 拖拽排序）
// ============================================================

/** 事件分类区块的编辑状态与回调（善业/恶业/善果三个同构区块复用）。 */
private class EventListUi(
    val title: String,
    val titleColor: Color,
    val presets: List<String>,
    val defaultScores: List<Float>,
    val addMode: Boolean,
    val deleteMode: Boolean,
    val onNameChange: (Int, String) -> Unit,
    val onScoreChange: (Int, Float) -> Unit,
    val onAddAfter: (Int) -> Unit,
    val onAddAtTop: () -> Unit,
    val onDelete: (Int) -> Unit,
    val onMove: (Int, Int) -> Unit,
    val onToggleAddMode: () -> Unit,
    val onToggleDeleteMode: () -> Unit,
)

@Composable
private fun EventSettingsCard(
    goodDeed: EventListUi,
    badDeed: EventListUi,
    goodResult: EventListUi,
) {
    SettingsCard("事件管理") {
        Text(
            "每个事件可设置默认分数：主页面点选事件时，左侧分数栏会自动移到该分数（之后仍可滑动调整）。" +
                "善业为正分，恶业/善果为负分（输入 1 即扣 1 分）。",
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        EventListBlock(goodDeed)

        Spacer(Modifier.height(16.dp))

        EventListBlock(badDeed)

        Spacer(Modifier.height(16.dp))

        EventListBlock(goodResult)
    }
}

/**
 * 一个事件分类区块（善业/恶业/善果同构）：
 * - 表头：标题 +（加模式下显示）+ 钮（在列表最上面插入，解决"删光后无法添加/不能加在最上面"）
 * - 行：事件名称输入 ｜ 默认分数输入（正数）｜（加模式绿色＋ / 减模式红色×）｜ ≡ 拖拽柄
 * - 底部：+ − 模式切换（同阶位：互斥）
 */
@Composable
private fun EventListBlock(ui: EventListUi) {
    val count = ui.presets.size
    // 长按进入拖动时震动提示（系统长按触感）
    val haptic = LocalHapticFeedback.current

    // 拖拽排序状态：长按 ≡ 后拖动；拖动中实时计算目标位，其他行动画让位
    var draggingIndex by remember { mutableStateOf<Int?>(null) }  // 被拖行原始下标
    var dragDy by remember { mutableFloatStateOf(0f) }             // 被拖行累计位移
    var currentTarget by remember { mutableStateOf(0) }            // 实时目标位（让位依据）
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    // 表头
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "■ ${ui.title}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ui.titleColor,
            modifier = Modifier.weight(1f),
        )
        if (ui.addMode) {
            // 表头 +：仅在加模式下显示，点击在列表最上面插入新事件
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22aa44))
                    .clickable { ui.onAddAtTop() },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    if (count == 0) {
        Text(
            "（暂无事件：按下方 + 进入添加模式，表头会出现 + 可在此分类最上面新增）",
            fontSize = 12.sp,
            color = TextMuted,
        )
    }

    for (i in 0 until count) {
        val isDragging = draggingIndex == i
        val fromIdx = draggingIndex
        // 实时让位：被拖行下方（向下拖）的行上移一行、上方（向上拖）的行下移一行，
        // 视觉上其他事件自动补位，松手后数据重排固定
        val shiftUp = !isDragging && fromIdx != null && i > fromIdx && i <= currentTarget
        val shiftDown = !isDragging && fromIdx != null && i < fromIdx && i >= currentTarget
        // 让位位移用动画平滑移动（不突变）；被拖行则实时跟随手指（无动画）
        val settleOffset by animateFloatAsState(
            targetValue = when {
                shiftUp -> -rowHeightPx
                shiftDown -> rowHeightPx
                else -> 0f
            },
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "eventSettle",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .graphicsLayer {
                    translationY = if (isDragging) dragDy else settleOffset
                }
                .zIndex(if (isDragging) 1f else 0f)
                .onSizeChanged { rowHeightPx = it.height.toFloat() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 事件名称（受控输入，值直接来自草稿）
            OutlinedTextField(
                value = ui.presets.getOrElse(i) { "" },
                onValueChange = { ui.onNameChange(i, it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ui.titleColor,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ui.titleColor,
                ),
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(6.dp))

            // 默认分数（显示正数；存储时按分类取符号）
            val score = ui.defaultScores.getOrElse(i) { if (ui.title == "善业") 1f else -1f }
            var scoreText by remember(i, score) { mutableStateOf(formatFloat(abs(score))) }
            OutlinedTextField(
                value = scoreText,
                onValueChange = { v ->
                    scoreText = v
                    v.toFloatOrNull()?.let { ui.onScoreChange(i, it) }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ui.titleColor,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = ui.titleColor,
                ),
                modifier = Modifier.widthIn(min = 52.dp),
            )

            // 加/减模式行内按钮（同阶位：+ 绿色 / × 红色）
            if (ui.addMode) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22aa44))
                        .clickable { ui.onAddAfter(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (ui.deleteMode) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFcc0000))
                        .clickable { ui.onDelete(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.width(4.dp))

            // ≡ 拖拽柄：三根等长横线，长按后变金色提示可拖动，上下拖动实时让位排序
            Column(
                modifier = Modifier
                    .width(22.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isDragging) Gold.copy(alpha = 0.25f)
                        else Color.Transparent
                    )
                    .pointerInput(i) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                // 长按成功：震动提示 + 手柄变金，进入可拖动状态
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                draggingIndex = i
                                currentTarget = i
                                dragDy = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDy += amount.y
                                // 实时更新目标位：越过半行高度即让位
                                if (rowHeightPx > 0f) {
                                    currentTarget = (i + (dragDy / rowHeightPx).roundToInt())
                                        .coerceIn(0, count - 1)
                                }
                            },
                            onDragEnd = {
                                if (currentTarget != i) ui.onMove(i, currentTarget)
                                draggingIndex = null
                                dragDy = 0f
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragDy = 0f
                            },
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(1.5.dp)
                            .padding(vertical = 0.dp)
                            .background(
                                if (isDragging) Gold else TextSecondary.copy(alpha = 0.8f)
                            ),
                    )
                    if (it < 2) Spacer(Modifier.height(3.dp))
                }
            }
        }
    }

    // 底部 +/− 模式切换（同阶位：+ 进入添加模式，− 进入删除模式）
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        val addActive = ui.addMode
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (addActive) Color(0xFF22aa44) else Color(0xFF1A1A1A))
                .border(1.dp, if (addActive) Color(0xFF22aa44) else Color(0xFF334444), RoundedCornerShape(6.dp))
                .clickable { ui.onToggleAddMode() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("+", fontSize = 16.sp, color = if (addActive) Color.White else Gold)
        }

        Spacer(Modifier.width(8.dp))

        val delActive = ui.deleteMode
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (delActive) Color(0xFFb8860b) else Color(0xFF1A1A1A))
                .border(1.dp, if (delActive) Color(0xFFb8860b) else Color(0xFF334444), RoundedCornerShape(6.dp))
                .clickable { ui.onToggleDeleteMode() }
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text("−", fontSize = 16.sp, color = if (delActive) Color.White else Gold)
        }
    }
}

// ============================================================
// HistorySettingsCard — 历史记录设置
// ============================================================

@Composable
private fun HistorySettingsCard(
    historyLineThickness: Float,
    historyDotRadius: Float,
    onLineThicknessChange: (Float) -> Unit,
    onDotRadiusChange: (Float) -> Unit,
) {
    SettingsCard("历史记录设置") {
        SettingsSlider("线条粗细", historyLineThickness, 0.5f..10f, 18, onLineThicknessChange)
        SettingsSlider("点的半径", historyDotRadius, 1f..16f, 29, onDotRadiusChange)
    }
}

// ============================================================
// DecaySettingsCard — 业力衰减
// ============================================================

@Composable
private fun DecaySettingsCard(
    decayEnabled: Boolean,
    decayHour: Int,
    decayMinute: Int,
    lastDecayDate: String,
    rankDecayAmounts: List<Float>,
    rankThresholds: List<Float>,
    rankNames: List<String>,
    onDecayEnabledChange: (Boolean) -> Unit,
    onDecayTimeChange: (Int, Int) -> Unit,
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
                checked = decayEnabled,
                onCheckedChange = onDecayEnabledChange,
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
                text = String.format("%02d:%02d", decayHour, decayMinute),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
        }

        if (showTimePicker) {
            TimePickerDialog(
                initialHour = decayHour,
                initialMinute = decayMinute,
                onConfirm = { hour, minute -> onDecayTimeChange(hour, minute) },
                onDismiss = { showTimePicker = false },
            )
        }

        Spacer(Modifier.height(12.dp))

        // 上次扣除日期
        Text(
            text = "上次扣除：${lastDecayDate.ifEmpty { "尚未扣除" }}",
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
    luckEnabled: Boolean,
    luckT: Float,
    luckB: Float,
    luckW: Float,
    onLuckEnabledChange: (Boolean) -> Unit,
    onLuckTChange: (Float) -> Unit,
    onLuckBChange: (Float) -> Unit,
    onLuckWChange: (Float) -> Unit,
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
                checked = luckEnabled,
                onCheckedChange = onLuckEnabledChange,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Gold,
                    checkedTrackColor = Gold.copy(alpha = 0.3f),
                ),
            )
        }

        if (luckEnabled) {
            Spacer(Modifier.height(12.dp))

            // T — 天数（多少天后只通过总和影响）
            Text("你觉得多少天以后的行为只能通过总和影响？", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            var luckTText by remember(luckT) { mutableStateOf(luckT.toInt().toString()) }
            OutlinedTextField(
                value = luckTText,
                onValueChange = { v ->
                    luckTText = v
                    v.toFloatOrNull()?.let {
                        if (it >= 1f && it <= 365f) onLuckTChange(it)
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
            var luckBText by remember(luckB) { mutableStateOf(formatFloat(luckB)) }
            OutlinedTextField(
                value = luckBText,
                onValueChange = { v ->
                    luckBText = v
                    v.toFloatOrNull()?.let {
                        if (it >= 0.1f && it <= 100f) onLuckBChange(it)
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
            var luckWText by remember(luckW) { mutableStateOf(formatFloat(luckW)) }
            OutlinedTextField(
                value = luckWText,
                onValueChange = { v ->
                    luckWText = v
                    v.toFloatOrNull()?.let {
                        if (it >= 1f && it <= 1000f) onLuckWChange(it)
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
            val aVal = 9.0 / (2.0 * luckT * luckT)
            val cIntegral = LuckAmplifier.integralExpMinusAt2(aVal, 0.0, 1.0)
            val cVal = luckW.toDouble() / (10.0 * cIntegral)
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
    themeGradientBaseColor: Long,
    themeGradientAccentColor: Long,
    onBaseColorChange: (Long) -> Unit,
    onAccentColorChange: (Long) -> Unit,
) {
    var showBaseColorPicker by remember { mutableStateOf(false) }
    var showAccentColorPicker by remember { mutableStateOf(false) }

    SettingsCard("背景渐变设置") {
        Text("渐变起始色（顶部）", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(color = themeGradientBaseColor, onClick = { showBaseColorPicker = true })

        Spacer(Modifier.height(12.dp))

        Text("渐变结束色（底部）", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        ColorSwatch(color = themeGradientAccentColor, onClick = { showAccentColorPicker = true })

        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Color(themeGradientBaseColor), Color(themeGradientAccentColor))))
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
        )
    }

    if (showBaseColorPicker) {
        ColorPickerDialog(
            currentColor = themeGradientBaseColor,
            onColorSelected = { onBaseColorChange(it); showBaseColorPicker = false },
            onDismiss = { showBaseColorPicker = false },
        )
    }
    if (showAccentColorPicker) {
        ColorPickerDialog(
            currentColor = themeGradientAccentColor,
            onColorSelected = { onAccentColorChange(it); showAccentColorPicker = false },
            onDismiss = { showAccentColorPicker = false },
        )
    }
}

// ============================================================
// SplashSettingsCard — 启动经文设置
// ============================================================

@Composable
private fun SplashSettingsCard(
    splashScripture: String,
    splashDurationSec: Int,
    onScriptureChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
) {
    SettingsCard("启动经文") {
        Text(
            "输入启动时显示的经文或境界语（金色竖排文字，自动缩放）",
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        Text("经文内容", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = splashScripture,
            onValueChange = onScriptureChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            singleLine = false,
            minLines = 3,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Serif,
                color = Color(0xFFffd700),
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = Color(0xFFffd700),
                unfocusedTextColor = Color(0xFFffd700),
                cursorColor = Gold,
            ),
        )

        Spacer(Modifier.height(16.dp))

        Text("停留秒数", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        var durationText by remember(splashDurationSec) { mutableStateOf(splashDurationSec.toString()) }
        OutlinedTextField(
            value = durationText,
            onValueChange = { v ->
                durationText = v
                v.toIntOrNull()?.let { onDurationChange(it.coerceIn(1, 30)) }
            },
            modifier = Modifier.widthIn(min = 70.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Gold,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text("建议 1~10 秒", fontSize = 11.sp, color = TextMuted)
    }
}

// ============================================================
// SelectiveResetCard — 选择性重置（勾选要重置的分组，确认后写入草稿，保存才生效）
// ============================================================

@Composable
private fun SelectiveResetCard(onReset: (Set<ResetGroup>) -> Unit) {
    // 默认全选：想「重置除了哪些」直接去掉不想重置的勾选；
    // 想「只重置哪些」先全不选再勾选目标分组
    var selected by remember { mutableStateOf(ResetGroup.entries.toSet()) }
    var showResetDialog by remember { mutableStateOf(false) }

    SettingsCard("选择性重置") {
        Text(
            "勾选要重置的分组，点「开始重置」后仍需按「保存设置」才生效",
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        // 全选 / 全不选
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { selected = ResetGroup.entries.toSet() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
            ) {
                Text("全选", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { selected = emptySet() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            ) {
                Text("全不选", fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        // 分组勾选列表
        ResetGroup.entries.forEach { group ->
            val checked = group in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selected = if (checked) selected - group else selected + group
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        selected = if (isChecked) selected + group else selected - group
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Gold,
                        uncheckedColor = BorderSubtle,
                        checkmarkColor = Color.Black,
                    ),
                )
                Text(
                    text = group.label,
                    fontSize = 14.sp,
                    color = if (checked) TextPrimary else TextMuted,
                    fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
                )
            }
            if (group != ResetGroup.entries.last()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderSubtle.copy(alpha = 0.15f)),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showResetDialog = true },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFff5252),
                disabledContentColor = Color(0xFF666666),
            ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("开始重置（已选 ${selected.size} 项）", fontFamily = FontFamily.Serif, fontSize = 14.sp)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置", color = Gold) },
            text = {
                Column {
                    Text("以下分组将恢复默认，此操作不可撤销：", color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = selected.joinToString("、") { it.label },
                        color = Color(0xFFff5252),
                        fontSize = 13.sp,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReset(selected)
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
}

// ============================================================
// Helper Composables
// ============================================================

@Composable
private fun SettingsCard(
    title: String,
    headerTrailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                modifier = Modifier.weight(1f),
            )
            if (headerTrailing != null) headerTrailing()
        }
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
    // 拖动期间使用本地状态，仅松手时 sync 到 ViewModel，避免每帧 copy(67字段)+重组
    var localValue by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value) {
        if (value != localValue) localValue = value
    }
    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    Spacer(Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChange(localValue) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Gold,
                activeTrackColor = Gold,
            ),
        )
        Text(
            formatFloat(localValue),
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
// PositiveRankSettingsCard — 正阶位体系（可变数量）
// ============================================================

@Composable
private fun PositiveRankSettingsCard(
    rankNames: List<String>,
    rankColors: List<Long>,
    rankThresholds: List<Float>,
    rankDecayAmounts: List<Float>,
    rankDivinationLimits: List<Int>,
    deleteMode: Boolean,
    addMode: Boolean,
    onRankNameChange: (Int, String) -> Unit,
    onRankColorChange: (Int, Long) -> Unit,
    onRankThresholdChange: (Int, Float) -> Unit,
    onRankDecayChange: (Int, Float) -> Unit,
    onRankDivinationLimitChange: (Int, Int) -> Unit,
    onAddRankAfter: (Int) -> Unit,
    onAddRankAtTop: () -> Unit,
    onDeleteRank: (Int) -> Unit,
    onToggleAddMode: () -> Unit,
    onToggleDeleteMode: () -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf(0) }

    SettingsCard(
        title = "正阶位体系",
        headerTrailing = if (addMode) {
            {
                // 表头 +：仅加模式下显示，点击在最上面插入新阶位（同事件管理）
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22aa44))
                        .clickable { onAddRankAtTop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else null,
    ) {
        val count = rankNames.size

        for (i in 0 until count) {
            val isLast = i == count - 1
            val name = rankNames.getOrElse(i) { "?" }
            val color = rankColors.getOrElse(i) { 0xFFFFFFFF }
            val threshold = rankThresholds.getOrElse(i) { 0f }
            val decay = rankDecayAmounts.getOrElse(i) { 2f }
            val divLimit = rankDivinationLimits.getOrElse(i) { 2 }

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
                    // 第一行：名称 + 添加/删除按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { onRankNameChange(i, it) },
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

                        // 添加按钮（仅 addMode 显示）
                        if (addMode) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22aa44))
                                    .clickable { onAddRankAfter(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "+", color = Color.White,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        // 删除按钮（仅 deleteMode 显示）
                        if (deleteMode) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFcc0000))
                                    .clickable { onDeleteRank(i) },
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
                                    v.toFloatOrNull()?.let { onRankThresholdChange(i, it) }
                                },
                                modifier = Modifier.widthIn(min = 52.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
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
                            onClick = { onRankDecayChange(i, decay - 1f) },
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
                            onClick = { onRankDecayChange(i, decay + 1f) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Text("+", fontSize = 16.sp, color = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // 第四行：每日占卜次数（UI 同业力衰减）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("占卜次数：", fontSize = 12.sp, color = TextMuted)
                        IconButton(
                            onClick = { onRankDivinationLimitChange(i, divLimit - 1) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Text("−", fontSize = 16.sp, color = TextSecondary)
                        }
                        Text(
                            divLimit.toString(),
                            fontSize = 14.sp, color = Gold,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(
                            onClick = { onRankDivinationLimitChange(i, divLimit + 1) },
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
            // [+] 添加模式切换
            val addActive = addMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (addActive) Color(0xFF22aa44) else Color(0xFF1A1A1A)
                    )
                    .border(
                        1.dp,
                        if (addActive) Color(0xFF22aa44) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onToggleAddMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "+", fontSize = 16.sp,
                    color = if (addActive) Color.White else Gold,
                )
            }

            Spacer(Modifier.width(8.dp))

            // [-] 删除模式切换
            val delActive = deleteMode && count > 1
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (delActive) Color(0xFFb8860b) else Color(0xFF1A1A1A)
                    )
                    .border(
                        1.dp,
                        if (delActive) Color(0xFFb8860b) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable(enabled = count > 1) { onToggleDeleteMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "−", fontSize = 16.sp,
                    color = when {
                        delActive -> Color.White
                        count <= 1 -> Color(0xFF666666)
                        else -> Gold
                    },
                )
            }
        }

    }

    // 颜色选择器（正阶）
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = rankColors.getOrElse(colorPickerTarget) { 0xFFFFFFFF },
            onColorSelected = {
                onRankColorChange(colorPickerTarget, it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }
}

// ============================================================
// NegativeRankSettingsCard — 负阶位体系（可变数量）
// ============================================================

@Composable
private fun NegativeRankSettingsCard(
    negativeRankNames: List<String>,
    negativeRankColors: List<Long>,
    negativeRankThresholds: List<Float>,
    negativeRankDivinationLimits: List<Int>,
    negativeAddMode: Boolean,
    negativeDeleteMode: Boolean,
    onNegativeRankNameChange: (Int, String) -> Unit,
    onNegativeRankColorChange: (Int, Long) -> Unit,
    onNegativeRankThresholdChange: (Int, Float) -> Unit,
    onNegativeRankDivinationLimitChange: (Int, Int) -> Unit,
    onAddNegativeRankAfter: (Int) -> Unit,
    onAddNegativeRankAtTop: () -> Unit,
    onDeleteNegativeRank: (Int) -> Unit,
    onToggleNegativeAddMode: () -> Unit,
    onToggleNegativeDeleteMode: () -> Unit,
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf(0) }

    SettingsCard(
        title = "负阶位体系",
        headerTrailing = if (negativeAddMode) {
            {
                // 表头 +：仅加模式下显示，点击在最上面插入新负阶（同事件管理）
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22aa44))
                        .clickable { onAddNegativeRankAtTop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else null,
    ) {
        Text(
            "下限即本级的业力下限（最深一级无下限）；颜色显示在首页徽章；占卜次数同正阶设置",
            fontSize = 12.sp,
            color = TextMuted,
        )
        Spacer(Modifier.height(8.dp))

        val negCount = negativeRankNames.size
        for (i in 0 until negCount) {
            val isLast = i == negCount - 1
            val name = negativeRankNames.getOrElse(i) { "?" }
            val color = negativeRankColors.getOrElse(i) { 0xFF000000L }
            val threshold = negativeRankThresholds.getOrElse(i) { 0f }
            val negLimit = negativeRankDivinationLimits.getOrElse(i) { 0 }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .border(1.dp, Color(0xFF000000).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Column {
                    // 第一行：名称 + 添加/删除按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { onNegativeRankNameChange(i, it) },
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
                        if (negativeAddMode) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22aa44))
                                    .clickable { onAddNegativeRankAfter(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (negativeDeleteMode) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFcc0000))
                                    .clickable { onDeleteNegativeRank(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // 第二行：颜色 + 下限
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorSwatch(color = color) {
                            colorPickerTarget = i
                            showColorPicker = true
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("下限：", fontSize = 12.sp, color = TextMuted)
                        if (isLast) {
                            Text("-∞", fontSize = 14.sp, color = TextMuted)
                        } else {
                            var negativeThresholdText by remember(threshold) {
                                mutableStateOf(formatFloat(threshold))
                            }
                            OutlinedTextField(
                                value = negativeThresholdText,
                                onValueChange = { v ->
                                    negativeThresholdText = v
                                    v.toFloatOrNull()?.let { onNegativeRankThresholdChange(i, it) }
                                },
                                modifier = Modifier.widthIn(min = 52.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                    // 第三行：每日占卜次数
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("占卜次数：", fontSize = 12.sp, color = TextMuted)
                        IconButton(
                            onClick = { onNegativeRankDivinationLimitChange(i, negLimit - 1) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Text("−", fontSize = 16.sp, color = TextSecondary)
                        }
                        Text(
                            negLimit.toString(),
                            fontSize = 14.sp, color = Gold,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(
                            onClick = { onNegativeRankDivinationLimitChange(i, negLimit + 1) },
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (negativeAddMode) Color(0xFF22aa44) else Color(0xFF1A1A1A))
                    .border(
                        1.dp,
                        if (negativeAddMode) Color(0xFF22aa44) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onToggleNegativeAddMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("+", fontSize = 16.sp, color = if (negativeAddMode) Color.White else Gold)
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (negativeDeleteMode) Color(0xFFb8860b) else Color(0xFF1A1A1A))
                    .border(
                        1.dp,
                        if (negativeDeleteMode) Color(0xFFb8860b) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onToggleNegativeDeleteMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("−", fontSize = 16.sp, color = when {
                    negativeDeleteMode -> Color.White
                    negCount <= 1 -> Color(0xFF666666)
                    else -> Gold
                })
            }
        }
    }

    // 颜色选择器（负阶）
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = negativeRankColors.getOrElse(colorPickerTarget) { 0xFF000000L },
            onColorSelected = {
                onNegativeRankColorChange(colorPickerTarget, it)
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

    // 从 HSV 导出 R/G/B 整数值（单次 HSVToColor，按位拆分）
    val currentRgb by remember(hue, saturation, value) {
        derivedStateOf {
            android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        }
    }
    val currentR = (currentRgb shr 16) and 0xFF
    val currentG = (currentRgb shr 8) and 0xFF
    val currentB = currentRgb and 0xFF

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
        title = { Text("选择颜色", fontFamily = FontFamily.Serif, color = Gold) },
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
            Text("色相", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif), color = TextSecondary)
            Text("${hue.toInt()}°", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
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
            Text("饱和度", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif), color = TextSecondary)
            Text("${(saturation * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
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
            Text("明度", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif), color = TextSecondary)
            Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif), color = TextPrimary)
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
        label = { Text(label, fontFamily = FontFamily.Serif, color = TextMuted) },
        modifier = modifier
            .widthIn(min = 56.dp)
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                if (!focusState.isFocused && text.isEmpty()) {
                    text = currentValue.toString()
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
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
        title = { Text("选择扣除时间", fontFamily = FontFamily.Serif, color = Gold) },
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

    // 选中值通过取模归一化到 base 范围
    LaunchedEffect(centerItemIndex) {
        onSelected(baseItems[centerItemIndex % baseSize])
    }

    // 停止滑动后自动吸附：让最近项对齐到视口中心。
    // 注意：必须用 snapshotFlow 而非 LaunchedEffect(isScrollInProgress) 作 key，
    // 否则 animateScrollToItem 触发 isScrollInProgress 变化 → LaunchedEffect 重启 → 协程被取消 → 动画中断。
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    delay(60)
                    val info = listState.layoutInfo
                    if (info.visibleItemsInfo.isEmpty()) return@collect
                    val vc = info.viewportEndOffset / 2
                    val closest = info.visibleItemsInfo.minByOrNull { i ->
                        abs((i.offset + i.size / 2) - vc)
                    } ?: return@collect
                    val diff = closest.offset + closest.size / 2f - vc
                    if (abs(diff) > 4f) {
                        // animateScrollToItem 把目标放顶部；减 visibleItems/2 位置 → 目标落到第 3 位 = 中心
                        val snapFirst = (closest.index - visibleItems / 2)
                            .coerceIn(0, totalSize - 1)
                        listState.animateScrollToItem(snapFirst)
                    }
                }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            scope.launch { listState.animateScrollToItem(index) }
                            onSelected(baseItems[value % baseSize])
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format("%02d", value),
                        fontSize = if (isCenter) 22.sp else 14.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) Color.White else TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ============================================================
// DailyMustDoCard — 每日必做
// ============================================================

@Composable
private fun DailyMustDoCard(
    goodDeedPresets: List<String>,
    dailyMustDoDeeds: List<DailyMustDoDeed>,
    onToggle: (String, Boolean) -> Unit,
    onPenaltyChange: (String, Float) -> Unit,
) {
    SettingsCard("每日必做") {
        Text(
            "勾选需要在每日完成的善业，并设置未完成扣分",
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        if (goodDeedPresets.isEmpty()) {
            Text(
                "（暂无善业预设，请先在「右边事件列表」中添加善业）",
                fontSize = 12.sp,
                color = TextMuted,
            )
        } else {
            goodDeedPresets.forEach { deed ->
                val matchingDeed = dailyMustDoDeeds.find { it.name == deed }
                val isDaily = matchingDeed != null
                val penalty = matchingDeed?.penalty ?: 1f
                var penaltyText by remember(deed, isDaily, penalty) {
                    mutableStateOf(if (isDaily) formatDailyPenalty(penalty) else "1")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 开关
                    Switch(
                        checked = isDaily,
                        onCheckedChange = { enabled -> onToggle(deed, enabled) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Gold,
                            checkedTrackColor = Gold.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    )

                    // 事件名称
                    Text(
                        text = deed,
                        fontSize = 13.sp,
                        color = if (isDaily) TextPrimary else TextMuted,
                        modifier = Modifier.weight(1f),
                    )

                    if (isDaily) {
                        Text(
                            "未完成扣",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        OutlinedTextField(
                            value = penaltyText,
                            onValueChange = { v ->
                                penaltyText = v
                                v.toFloatOrNull()?.let { onPenaltyChange(deed, it) }
                            },
                            modifier = Modifier.widthIn(min = 52.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4488ff),
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Color(0xFF4488ff),
                            ),
                        )
                        Text("分", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(start = 2.dp))
                    } else {
                        // 未启用时显示灰色占位
                        Text(
                            "未完成扣_分",
                            fontSize = 11.sp,
                            color = TextMuted.copy(alpha = 0.4f),
                        )
                    }
                }

                // 分割线
                if (deed != goodDeedPresets.last()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BorderSubtle.copy(alpha = 0.15f)),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "（只有善业可以被设为每日必做）",
            fontSize = 11.sp,
            color = TextMuted.copy(alpha = 0.6f),
        )
    }
}

private fun formatDailyPenalty(v: Float): String {
    return if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)
}

// ============================================================
// MultiplierSettingsCard — 乘法功能（事件管理式列表：表头+、数值行、加/减模式、拖拽排序）
// ============================================================

@Composable
private fun MultiplierSettingsCard(
    multipliers: List<Float>,
    addMode: Boolean,
    deleteMode: Boolean,
    onValueChange: (Int, Float) -> Unit,
    onAddAfter: (Int) -> Unit,
    onAddAtTop: () -> Unit,
    onDelete: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onToggleAddMode: () -> Unit,
    onToggleDeleteMode: () -> Unit,
) {
    val count = multipliers.size
    // 长按进入拖动时震动提示（系统长按触感）
    val haptic = LocalHapticFeedback.current

    // 拖拽排序状态：长按 ≡ 后拖动；拖动中实时计算目标位，其他行动画让位
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    var currentTarget by remember { mutableStateOf(0) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    SettingsCard("乘法功能") {
        Text(
            "主页面左栏的乘法按钮：点击后当前分数 × 该数，结果向 0.5 四舍五入（不限幅）。",
            fontSize = 12.sp,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        // 表头（加模式下显示 +，点击在最上面插入）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "■ 乘法按钮",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
                modifier = Modifier.weight(1f),
            )
            if (addMode) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22aa44))
                        .clickable { onAddAtTop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (count == 0) {
            Text(
                "（暂无乘法按钮：按下方 + 进入添加模式，表头会出现 + 可在此分类最上面新增）",
                fontSize = 12.sp,
                color = TextMuted,
            )
        }

        for (i in 0 until count) {
            val isDragging = draggingIndex == i
            val fromIdx = draggingIndex
            // 实时让位：被拖行下方（向下拖）的行上移一行、上方（向上拖）的行下移一行
            val shiftUp = !isDragging && fromIdx != null && i > fromIdx && i <= currentTarget
            val shiftDown = !isDragging && fromIdx != null && i < fromIdx && i >= currentTarget
            val settleOffset by animateFloatAsState(
                targetValue = when {
                    shiftUp -> -rowHeightPx
                    shiftDown -> rowHeightPx
                    else -> 0f
                },
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "multiplierSettle",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .graphicsLayer {
                        translationY = if (isDragging) dragDy else settleOffset
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .onSizeChanged { rowHeightPx = it.height.toFloat() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 乘法数值输入（受控输入，值直接来自草稿）
                val value = multipliers.getOrElse(i) { 1f }
                var valueText by remember(i, value) { mutableStateOf(formatMultiplierValue(value)) }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { v ->
                        valueText = v
                        v.toFloatOrNull()?.let { onValueChange(i, it) }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Gold,
                    ),
                    modifier = Modifier.weight(1f),
                )

                // 加/减模式行内按钮（同事件/阶位：+ 绿色 / × 红色）
                if (addMode) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22aa44))
                            .clickable { onAddAfter(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (deleteMode) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFcc0000))
                            .clickable { onDelete(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.width(4.dp))

                // ≡ 拖拽柄：三根等长横线，长按后变金色提示可拖动，上下拖动实时让位排序
                Column(
                    modifier = Modifier
                        .width(22.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isDragging) Gold.copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .pointerInput(i) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingIndex = i
                                    currentTarget = i
                                    dragDy = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragDy += amount.y
                                    // 实时更新目标位：越过半行高度即让位
                                    if (rowHeightPx > 0f) {
                                        currentTarget = (i + (dragDy / rowHeightPx).roundToInt())
                                            .coerceIn(0, count - 1)
                                    }
                                },
                                onDragEnd = {
                                    if (currentTarget != i) onMove(i, currentTarget)
                                    draggingIndex = null
                                    dragDy = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragDy = 0f
                                },
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(1.5.dp)
                                .background(
                                    if (isDragging) Gold else TextSecondary.copy(alpha = 0.8f)
                                ),
                        )
                        if (it < 2) Spacer(Modifier.height(3.dp))
                    }
                }
            }
        }

        // 底部 +/− 模式切换（同事件：+ 进入添加模式，− 进入删除模式）
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            val addActive = addMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (addActive) Color(0xFF22aa44) else Color(0xFF1A1A1A))
                    .border(
                        1.dp,
                        if (addActive) Color(0xFF22aa44) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onToggleAddMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "+", fontSize = 16.sp,
                    color = if (addActive) Color.White else Gold,
                )
            }

            Spacer(Modifier.width(8.dp))

            val delActive = deleteMode && count > 0
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (delActive) Color(0xFFb8860b) else Color(0xFF1A1A1A))
                    .border(
                        1.dp,
                        if (delActive) Color(0xFFb8860b) else Color(0xFF334444),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable(enabled = count > 0) { onToggleDeleteMode() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "−", fontSize = 16.sp,
                    color = when {
                        delActive -> Color.White
                        count <= 0 -> Color(0xFF666666)
                        else -> Gold
                    },
                )
            }
        }
    }
}

/** 乘法数值显示：1/3、2/3、1/4、1/2、3/4 显示分数，其余整数去点、小数最多两位。 */
private fun formatMultiplierValue(v: Float): String {
    val frac = when {
        abs(v - 1f / 3f) < 0.001f -> "1/3"
        abs(v - 2f / 3f) < 0.001f -> "2/3"
        abs(v - 1f / 4f) < 0.001f -> "1/4"
        abs(v - 1f / 2f) < 0.001f -> "1/2"
        abs(v - 3f / 4f) < 0.001f -> "3/4"
        else -> null
    }
    if (frac != null) return frac
    return if (v % 1f == 0f) v.toInt().toString()
    else String.format("%.2f", v).trimEnd('0').trimEnd('.')
}

// ============================================================
// Format Helper
// ============================================================

private fun formatFloat(v: Float): String {
    return if (v % 1f == 0f) v.toInt().toString() else String.format("%.1f", v)
}
