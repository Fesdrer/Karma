# Karma v3.1 — 分步实现指南

> 本文供 AI 按顺序逐步执行。每步包含：目标文件、替换代码、注意事项。
> 建议执行顺序：步骤 1→2→3→4→5→6（由简到繁，避免依赖混乱）。

---

## 步骤 1：设置 — 重置排除事件列表

### 1a. SettingsViewModel.kt

**文件：** `app/src/main/java/com/example/karma/ui/settings/SettingsViewModel.kt`
**位置：** 约第 186-193 行，`resetToDefaults()` 方法

**改动：** 在 `copy()` 中新增保留三个事件列表字段

**改后代码：**
```kotlin
fun resetToDefaults() {
    val current = _draft.value
    setDraft(KarmaSettingsEntity().copy(
        totalScore = current.totalScore,
        lastDecayDate = current.lastDecayDate,
        // ★ 新增：保留三个事件列表
        goodDeedPresets = current.goodDeedPresets,
        badDeedPresets = current.badDeedPresets,
        goodResultPresets = current.goodResultPresets,
    ))
}
```

**验证：** 确认 `resetToDefaults()` 被调用时，`_draft.value.goodDeedPresets` / `badDeedPresets` / `goodResultPresets` 不被重置为 `KarmaSettingsEntity()` 的默认值。

---

### 1b. SettingsScreen.kt

**文件：** `app/src/main/java/com/example/karma/ui/settings/SettingsScreen.kt`
**位置：** 约第 613-623 行，`ResetCard` 中的 `OutlinedButton`

**改动：** 按钮文字改为两行，第二行 "（事件除外）" 居中

**改后代码：**
```kotlin
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
            Text(
                "（事件除外）",
                fontSize = 11.sp,
                color = Color(0xFFff5252).copy(alpha = 0.7f),
            )
        }
    }
}
```

**注意：** 需要 import `Column` 和 `Alignment`（通常已 import）。确保 `Column` 的 `horizontalAlignment` 正确设置为 `Alignment.CenterHorizontally`。

---

## 步骤 2：EventPanel — 自定义事件字体调小

### EventPanel.kt

**文件：** `app/src/main/java/com/example/karma/ui/main/components/EventPanel.kt`
**位置：** 约第 110 行，`placeholder` 的 `Text` 组件

**改动：** `fontSize = 14.sp` → `fontSize = 12.sp`

**改后代码：**
```kotlin
placeholder = { Text("自定义事件...", fontSize = 12.sp, color = Color(0xFF666666)) },
```

**验证：** "自定义事件..." 文本在窄屏上不再换行。

---

## 步骤 3：分数面板 — 显示当前选中分数

### 3a. MainViewModel.kt

**文件：** `app/src/main/java/com/example/karma/ui/main/MainViewModel.kt`

**改动 A：** `MainUiState` data class 新增 `effectiveScore` 字段（约第 15-42 行）

```kotlin
data class MainUiState(
    val totalScore: Float = 0f,
    val rank: Rank? = null,
    val scorePresets: List<Float> = emptyList(),
    val goodDeedPresets: List<String> = emptyList(),
    val badDeedPresets: List<String> = emptyList(),
    val goodResultPresets: List<String> = emptyList(),
    val selectedScore: Float? = null,
    val selectedEvent: String? = null,
    val effectiveScore: Float? = null,   // ★ 新增：实际选中的分数（含自定义）
    // ... 其余字段不变 ...
)
```

**改动 B：** 在 `combine` 回调（约第 66-98 行）中赋值 `effectiveScore`：

```kotlin
val uiState: StateFlow<MainUiState> = combine(
    repository.settings,
    _selectedPair,
    _message,
) { settings, selection, msg ->
    MainUiState(
        totalScore = settings.totalScore,
        rank = repository.getRank(settings.totalScore),
        scorePresets = settings.scorePresets,
        goodDeedPresets = settings.goodDeedPresets,
        badDeedPresets = settings.badDeedPresets,
        goodResultPresets = settings.goodResultPresets,
        selectedScore = selection.first,
        selectedEvent = selection.second,
        effectiveScore = selection.first,   // ★ 新增
        // ... 其余字段不变 ...
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())
```

**原因：** `selection.first` 来自 `_effectiveScore`（`_customScore ?: _selectedScore`），能正确反映无论是点击选择还是自定义输入的有效分数。而 `selectedScore` 只跟踪点击选择的分数。

---

### 3b. ScorePanel.kt

**文件：** `app/src/main/java/com/example/karma/ui/main/components/ScorePanel.kt`
**位置：** 约第 57-68 行，在 "分数" Title 与 Spacer 之间

**改动：** 在 Title 下方插入选中分数显示区域

**改后代码（第 57-78 行区域）：**
```kotlin
Column(
    modifier = modifier
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 8.dp, vertical = 8.dp),
) {
    // Title
    Text(
        text = "分数",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFffd700),
        modifier = Modifier.fillMaxWidth(),
    )

    // ★ 新增：当前选中分数显示
    if (selectedScore != null) {
        val displayText = if (selectedScore % 1f == 0f) {
            (if (selectedScore > 0) "+" else "") + selectedScore.toInt().toString()
        } else {
            (if (selectedScore > 0) "+" else "") + String.format("%.1f", selectedScore)
        }
        val scoreColor = when {
            selectedScore > 0 -> Color(0xFF69f0ae)   // 正分绿色
            selectedScore < 0 -> Color(0xFFff5252)   // 负分红色
            else -> Color(0xFFa0c4ff)                // 零分蓝色
        }
        Text(
            text = displayText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = scoreColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
    // ★ 结束新增

    Spacer(Modifier.height(4.dp))

    // Vertical axis canvas (后续代码不变)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        ScoreAxisView(...)
    }
    // ...
}
```

### 3c. MainScreen.kt — 传递 effectiveScore

**文件：** `app/src/main/java/com/example/karma/ui/main/MainScreen.kt`

**改动：** 将 `state.selectedScore` 改为 `state.effectiveScore`

```kotlin
// 改前（约第 78 行）：
ScorePanel(
    selectedScore = state.selectedScore,

// 改后：
ScorePanel(
    selectedScore = state.effectiveScore,
```

**验证：**
- 点击分数轴：顶部显示选中值（如 "+1.5"），正分绿色、负分红色
- 自定义输入分数：顶部也显示输入值
- 未选择时：该区域不渲染（`selectedScore != null` 判断）
- 视觉上明显小于 Header 的 28.sp 金色总分

---

## 步骤 4：历史页面 — ViewModel 核心逻辑

### HistoryViewModel.kt

**文件：** `app/src/main/java/com/example/karma/ui/history/HistoryViewModel.kt`

> ⚠️ 这是最复杂的改动，涉及数据流重构。建议逐段替换，不要一次性替换整个文件。

#### 4a. 新增 import

```kotlin
// 无需新增 import，Calendar 和 ViewMode 均已导入
```

#### 4b. HistoryUiState 新增字段

**替换整个 data class**（约第 21-28 行）：

```kotlin
data class HistoryUiState(
    val entries: List<HistoryEntryEntity> = emptyList(),
    val viewMode: ViewMode = ViewMode.DAY,
    val aggregatedPoints: List<AggregatedPoint> = emptyList(),
    val historyLineThickness: Float = 2f,
    val historyDotRadius: Float = 3.5f,
    val message: String? = null,
    // ★ 以下为新增字段
    val focusDate: Long = System.currentTimeMillis(),
    val canGoForward: Boolean = false,
    val dateLabel: String = "",
    val isZoomEnabled: Boolean = false,
    val rankColors: List<Long> = emptyList(),
)
```

#### 4c. ViewModel 类新增状态

在 `HistoryViewModel` 类内部，`_message` 之后新增（约第 44 行后）：

```kotlin
// ★ 新增状态
private val _focusDate = MutableStateFlow(System.currentTimeMillis())
private val _isZoomEnabled = MutableStateFlow(false)
```

#### 4d. 重写 combine 流

**替换整个 `uiState` 定义**（约第 45-59 行）：

```kotlin
val uiState: StateFlow<HistoryUiState> = combine(
    repository.allHistory,
    repository.settings,
    _viewMode.asStateFlow(),
    _message.asStateFlow(),
    _focusDate.asStateFlow(),
    _isZoomEnabled.asStateFlow(),
) { entries, settings, mode, msg, focusDate, zoomEnabled ->
    HistoryUiState(
        entries = entries,
        viewMode = mode,
        aggregatedPoints = aggregate(entries, mode, focusDate),
        historyLineThickness = settings.historyLineThickness,
        historyDotRadius = settings.historyDotRadius,
        focusDate = focusDate,
        canGoForward = !isAtNewest(focusDate, mode),
        dateLabel = formatDateLabel(focusDate, mode),
        isZoomEnabled = zoomEnabled,
        rankColors = settings.rankColors,
        message = msg,
    )
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
```

**注意：** combine 现在有 6 个参数（原来是 4 个），每个参数的 lambda 位置要对应正确。

#### 4e. 重写 aggregate() 方法

**替换整个 `aggregate` 方法**（约第 69-98 行）：

```kotlin
private fun aggregate(
    entries: List<HistoryEntryEntity>,
    mode: ViewMode,
    focusDate: Long,
): List<AggregatedPoint> {
    if (entries.isEmpty()) return emptyList()
    val cal = Calendar.getInstance()

    val (startMs, endMs) = when (mode) {
        ViewMode.DAY -> {
            cal.timeInMillis = focusDate
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val end = cal.timeInMillis
            Pair(start, end)
        }
        ViewMode.WEEK -> {
            cal.timeInMillis = focusDate
            // 中国习惯：周一为一周开始
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            val end = cal.timeInMillis
            Pair(start, end)
        }
        ViewMode.MONTH -> {
            cal.timeInMillis = focusDate
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val end = cal.timeInMillis
            Pair(start, end)
        }
        ViewMode.ALL -> Pair(0L, Long.MAX_VALUE)
    }

    return entries
        .filter { it.timestamp in startMs until endMs }
        .map { it.toAggregated() }
}
```

#### 4f. 新增导航方法

在 `setViewMode` / `clearMessage` 之后、`private fun aggregate` 之前插入（约第 67 行处）：

```kotlin
// ★ 新增：导航方法
fun navigatePrevious() {
    if (_viewMode.value == ViewMode.ALL) return
    val cal = Calendar.getInstance().apply { timeInMillis = _focusDate.value }
    when (_viewMode.value) {
        ViewMode.DAY -> cal.add(Calendar.DAY_OF_MONTH, -1)
        ViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
        ViewMode.MONTH -> cal.add(Calendar.MONTH, -1)
        ViewMode.ALL -> return
    }
    _focusDate.value = cal.timeInMillis
}

fun navigateNext() {
    if (_viewMode.value == ViewMode.ALL) return
    if (isAtNewest(_focusDate.value, _viewMode.value)) return
    val cal = Calendar.getInstance().apply { timeInMillis = _focusDate.value }
    when (_viewMode.value) {
        ViewMode.DAY -> cal.add(Calendar.DAY_OF_MONTH, 1)
        ViewMode.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, 1)
        ViewMode.MONTH -> cal.add(Calendar.MONTH, 1)
        ViewMode.ALL -> return
    }
    _focusDate.value = cal.timeInMillis
}

fun resetFocusToToday() {
    _focusDate.value = System.currentTimeMillis()
}

fun toggleZoom() {
    _isZoomEnabled.value = !_isZoomEnabled.value
}

fun zoomToPoint(timestamp: Long) {
    when (_viewMode.value) {
        ViewMode.DAY -> {}  // 已是最细粒度
        ViewMode.WEEK -> {
            _focusDate.value = timestamp
            _viewMode.value = ViewMode.DAY
        }
        ViewMode.MONTH -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            _focusDate.value = cal.timeInMillis
            _viewMode.value = ViewMode.WEEK
        }
        ViewMode.ALL -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            _focusDate.value = cal.timeInMillis
            _viewMode.value = ViewMode.MONTH
        }
    }
}
```

#### 4g. 新增辅助方法

在 `aggregate()` 方法之后、`// ---- Export ----` 注释之前插入：

```kotlin
    // ---- 辅助方法 ----

    /** 判断 focusDate 是否在当前最新周期内 */
    private fun isAtNewest(focusDate: Long, mode: ViewMode): Boolean {
        if (mode == ViewMode.ALL) return true
        val now = Calendar.getInstance()
        val focus = Calendar.getInstance().apply { timeInMillis = focusDate }
        return when (mode) {
            ViewMode.DAY -> focus.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    && focus.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            ViewMode.WEEK -> focus.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR)
                    && focus.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            ViewMode.MONTH -> focus.get(Calendar.MONTH) == now.get(Calendar.MONTH)
                    && focus.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            ViewMode.ALL -> true
        }
    }

    /** 格式化日期标签 */
    private fun formatDateLabel(focusDate: Long, mode: ViewMode): String {
        if (mode == ViewMode.ALL) return "全部记录"
        val cal = Calendar.getInstance().apply { timeInMillis = focusDate }
        return when (mode) {
            ViewMode.DAY -> String.format("%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            ViewMode.WEEK -> {
                val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
                val monCal = cal.clone() as Calendar
                monCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val sunCal = cal.clone() as Calendar
                sunCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                "第${weekNum}周 (${monCal.get(Calendar.MONTH)+1}/${monCal.get(Calendar.DAY_OF_MONTH)}-${sunCal.get(Calendar.MONTH)+1}/${sunCal.get(Calendar.DAY_OF_MONTH)})"
            }
            ViewMode.MONTH -> String.format("%04d年%02d月",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            ViewMode.ALL -> "全部记录"
        }
    }
```

**请特别注意：** 确保这些辅助方法在类的结构体内、export 方法之前。保持缩进一致。

---

## 步骤 5：历史页面 — UI 导航栏

### HistoryScreen.kt

**文件：** `app/src/main/java/com/example/karma/ui/history/HistoryScreen.kt`

#### 5a. 修改 onPointClicked 回调

**位置：** 约第 211-221 行

**改后代码：**
```kotlin
HistoryChartCanvas(
    points = state.aggregatedPoints,
    viewport = viewport,
    lineThickness = state.historyLineThickness,
    dotRadius = state.historyDotRadius,
    rankColors = state.rankColors,   // ★ 新增：传入 rankColors
    onPointClicked = { point, screenX, screenY ->
        if (point != null) {
            if (state.isZoomEnabled && state.viewMode != ViewMode.DAY) {
                // ★ 缩放模式：下钻，不显示 tooltip
                viewModel.zoomToPoint(point.timestamp)
                showTooltip = false
                tooltipPoint = null
            } else {
                // 普通模式：显示 tooltip（原有逻辑不变）
                tooltipPoint = point
                tooltipX = screenX
                tooltipY = screenY
                showTooltip = true
            }
        } else {
            showTooltip = false
            tooltipPoint = null
        }
    },
    modifier = Modifier.fillMaxSize(),
)
```

#### 5b. 替换第二行（导出/导入）为导航栏 + 导出

**位置：** 从 `// Row 2: Export / Import buttons` 注释开始（约第 152 行）到该 `Row` 结束（约第 189 行），整段替换为：

```kotlin
        // ════════════════════════════════════════════════════════════
        // Row 2: 导航栏（◀ 日期 ▶ [今天] [放大]）+ 导出/导入
        // ════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // ── 左半：导航控件（ALL 模式下隐藏导航，仅显示放大开关） ──
            if (state.viewMode != ViewMode.ALL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ◀ 按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1a1a3e))
                            .border(1.dp, Color(0xFF334444), RoundedCornerShape(6.dp))
                            .clickable { viewModel.navigatePrevious() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text("<", fontSize = 14.sp, color = Color(0xFFa0c4ff))
                    }

                    Spacer(Modifier.width(6.dp))

                    // 日期标签
                    Text(
                        text = state.dateLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )

                    Spacer(Modifier.width(6.dp))

                    // ▶ 按钮（根据 canGoForward 切换样式和点击性）
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (state.canGoForward) Color(0xFF1a1a3e)
                                else Color(0xFF1a1a3e).copy(alpha = 0.4f)
                            )
                            .border(
                                1.dp,
                                if (state.canGoForward) Color(0xFF334444)
                                else Color(0xFF334444).copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(enabled = state.canGoForward) { viewModel.navigateNext() }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            ">", fontSize = 14.sp,
                            color = if (state.canGoForward) Color(0xFFa0c4ff) else Color(0xFF666666),
                        )
                    }

                    Spacer(Modifier.width(6.dp))

                    // [今天] 按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1a1a3e))
                            .border(1.dp, Color(0xFF334444), RoundedCornerShape(6.dp))
                            .clickable { viewModel.resetFocusToToday() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text("今天", fontSize = 11.sp, color = Color(0xFFa0c4ff))
                    }

                    Spacer(Modifier.width(8.dp))

                    // [放大] 开关
                    val zoomDisabled = state.viewMode == ViewMode.DAY
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (state.isZoomEnabled) Color(0xFF4a90d9)
                                else Color(0xFF1a1a3e)
                            )
                            .border(
                                1.dp,
                                when {
                                    state.isZoomEnabled -> Color(0xFF4a90d9)
                                    zoomDisabled -> Color(0xFF334444).copy(alpha = 0.2f)
                                    else -> Color(0xFF334444)
                                },
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(enabled = !zoomDisabled) { viewModel.toggleZoom() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            "放大", fontSize = 11.sp,
                            color = when {
                                zoomDisabled -> Color(0xFF666666)
                                state.isZoomEnabled -> Color.White
                                else -> Color(0xFFa0c4ff)
                            },
                        )
                    }
                }
            } else {
                // ALL 模式下只显示放大开关（无日期导航）
                val zoomDisabled = false
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (state.isZoomEnabled) Color(0xFF4a90d9)
                            else Color(0xFF1a1a3e)
                        )
                        .border(
                            1.dp,
                            if (state.isZoomEnabled) Color(0xFF4a90d9) else Color(0xFF334444),
                            RoundedCornerShape(6.dp),
                        )
                        .clickable { viewModel.toggleZoom() }
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(
                        "放大", fontSize = 11.sp,
                        color = if (state.isZoomEnabled) Color.White else Color(0xFFa0c4ff),
                    )
                }
            }

            // ── 右半：导出/导入按钮（保持不变） ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1a1a3e))
                        .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                        .clickable { exportCsvLauncher.launch("karma_data.csv") }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("CSV", fontSize = 13.sp, color = Color(0xFFa0c4ff))
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1a1a3e))
                        .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                        .clickable { exportJsonLauncher.launch("karma_data.json") }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("JSON", fontSize = 13.sp, color = Color(0xFFa0c4ff))
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1a1a3e))
                        .border(1.dp, Color(0xFF334444), RoundedCornerShape(8.dp))
                        .clickable { importLauncher.launch(arrayOf("application/json", "text/csv")) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("导入", fontSize = 13.sp, color = Color(0xFFa0c4ff))
                }
            }
        }
```

**注意：** 整段替换后，`Spacer(Modifier.height(12.dp))`（约第 191 行）保持不变，它分隔第二行和图表。

---

## 步骤 6：历史图表 — 阶位色带

### HistoryChartCanvas.kt

**文件：** `app/src/main/java/com/example/karma/ui/history/HistoryChartCanvas.kt`

#### 6a. 修改函数签名，新增 rankColors 参数

**位置：** 约第 32-40 行

```kotlin
@Composable
fun HistoryChartCanvas(
    points: List<AggregatedPoint>,
    viewport: ChartViewport,
    lineThickness: Float = 2f,
    dotRadius: Float = 3.5f,
    rankColors: List<Long> = emptyList(),       // ★ 新增
    onPointClicked: ((point: AggregatedPoint?, screenX: Float, screenY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

#### 6b. 绘制阶位色带

**位置：** 在 `drawRect(color = ChartBg, size = size)`（背景绘制）之后、`// ---- Grid lines ----` 注释之前插入。

**插入代码：**
```kotlin
        // ---- Rank color bands（阶位横向色带） ----
        if (rankColors.isNotEmpty() && points.size >= 2) {
            val rankList = com.example.karma.data.model.Rank.RANKS
            for (rank in rankList) {
                val bandColor = rankColors.getOrElse(rank.level - 1) { rank.colorHex }
                // rank.max 为高阶（分数高），rank.min 为低阶（分数低）
                // yMap: 高分 → 画布上方（y 值小），低分 → 画布下方（y 值大）
                val bandTopY = yMap(rank.max)     // 色带顶边（画布 Y 坐标）
                val bandBottomY = yMap(rank.min)   // 色带底边（画布 Y 坐标）

                // 裁切到可见绘图区域
                val drawTop = bandTopY.coerceIn(PAD_TOP, h - PAD_BOTTOM)
                val drawBottom = bandBottomY.coerceIn(PAD_TOP, h - PAD_BOTTOM)
                val bandHeight = drawBottom - drawTop
                if (bandHeight <= 0f) continue

                drawRect(
                    color = Color(bandColor).copy(alpha = 0.10f),
                    topLeft = Offset(PAD_LEFT, drawTop),
                    size = androidx.compose.ui.geometry.Size(plotW, bandHeight),
                )
            }
        }
```

**注意事项：**
- `yMap` 和 `plotW` 在 Viewport auto-fit 后定义，此时可用
- 透明度 `0.10f`：极淡，不会遮挡数据线条
- `drawRect` 在背景之上、网格线之下，符合视觉层叠顺序
- 色带仅绘制在 `PAD_LEFT` 到 `w - PAD_RIGHT` 的绘图区，不覆盖坐标轴标注
- 当 `points.size < 2`（数据不足）时，色带也不显示

---

## 编译与验证

### 编译

```bash
cd f:/1、学习/1、c++/作品/Karma
./gradlew assembleDebug
```

如果报编译错误，检查：
1. import 是否完整（`Calendar`、`Alignment`、`Column`、`Size` 等）
2. 语法错误（逗号、括号不匹配）
3. `combine` 的参数数量与 lambda 参数位置对应

### 功能验证清单

| 验证项 | 操作 | 预期结果 |
|--------|------|---------|
| ◀ 导航 | 在日/周/月模式下点击 ◀ | 日期标签更新，图表数据变化 |
| ▶ 导航 | 非最新日期时点击 ▶ | 向前推进，到达最新日期后按钮置灰 |
| 今天 | 导航到过去后点击「今天」 | 回到当天/周/月 |
| 全隐藏导航 | 切换到 ALL 模式 | 导航控件消失，仅剩放大开关 |
| 放大 - 周→日 | 周模式+放大开，点击数据点 | 切换到日模式，显示该天数据 |
| 放大 - 月→周 | 月模式+放大开，点击数据点 | 切换到周模式，显示该周数据 |
| 放大 - 全部→月 | 全部模式+放大开，点击数据点 | 切换到月模式，显示该月数据 |
| 放大不可用 | 日模式 | 放大按钮灰色不可点击 |
| 阶位色带 | 浏览历史图表 | 背景有半透明横向色带 |
| 选中分数 | 点击/拖动分数轴 | 顶部显示 "+1.5" 等 |
| 自定义事件单行 | 查看 EventPanel | "自定义事件..." 不换行 |
| 重置排除事件 | 修改事件→重置 | 事件列表保留，其他恢复默认 |
| 按钮两行 | 查看设置页底部 | "（事件除外）" 居中小字 |

---

## 附录：文件修改摘要

```
app/src/main/java/com/example/karma/
├── ui/
│   ├── history/
│   │   ├── HistoryViewModel.kt      ← 核心重构：+focusDate/+zoom/+dateLabel/重写aggregate
│   │   ├── HistoryScreen.kt         ← UI：导航栏替换导出行、点击缩放逻辑
│   │   └── HistoryChartCanvas.kt    ← 绘制：+rankColors参数、+rank色带
│   ├── main/
│   │   ├── MainViewModel.kt         ← 数据：+effectiveScore字段
│   │   ├── MainScreen.kt            ← 传参：selectedScore→effectiveScore
│   │   └── components/
│   │       ├── ScorePanel.kt        ← UI：+选中分数显示
│   │       └── EventPanel.kt        ← 微调：14.sp→12.sp
│   └── settings/
│       ├── SettingsViewModel.kt     ← 逻辑：reset保留事件列表
│       └── SettingsScreen.kt        ← UI：按钮两行居中
```

共 **8 个文件** 修改，建议按步骤 1→2→3→4→5→6 逐个进行，每步完成后编译确认无错再继续。
