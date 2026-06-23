# Karma App — 7 项问题修复计划（最终版 v3）

## Context

Kotlin + Jetpack Compose + Material3 + Room 构建的业力值模拟器。修复 7 个问题。

---

## 问题 1：快速点击返回 → 只剩背景色

### 根因
`HistoryScreen.kt:97` — `onBack()` 无防抖，快速连点 pop 多层。

### 修复
加防抖标记：
```kotlin
var backHandled by remember { mutableStateOf(false) }
.clickable { if (!backHandled) { backHandled = true; onBack() } }
```

**文件：** `HistoryScreen.kt`

---

## 问题 2：周显示范围错误（"6月7日到6日"）

### 根因
`Calendar.set(DAY_OF_WEEK, SUNDAY)` 在中国 locale 下返回前一个周日。

### 修复
"先算周一，再 +6 天"：
- `formatDateLabel()` 第 230-236 行
- `aggregate()` 第 113-122 行
- `zoomToPoint()` 第 188 行

**文件：** `HistoryViewModel.kt`

---

## 问题 3：加入"缩小"按钮

### 修复
1. `HistoryViewModel.kt` — `zoomOut()`：DAY→WEEK→MONTH→ALL
2. `HistoryScreen.kt` — "放大"旁加"缩小"，ALL 时禁用

**文件：** `HistoryViewModel.kt`, `HistoryScreen.kt`

---

## 问题 4：历史 tooltip 长事件名溢出 + 靠右时超出屏幕

### 原则
不限制行数。只约束最大宽度 + 智能定位。

### 修复

**A. ChartTooltip.kt：**
```kotlin
Column(
    modifier = modifier
        .widthIn(max = 220.dp)   // 约束最大宽度，防止无限拉长
        .background(...)
        ...
)
// 事件值：不设 maxLines，让其自由换行
Text(
    text = value,
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    color = valueColor,
    modifier = Modifier.weight(1f),  // 占据 Row 剩余空间
    // softWrap 默认 true，自动换行
)
```

**B. HistoryScreen.kt — 智能左右定位：**
```kotlin
val tooltipMaxWidth = 220.dp

// 优先放点右侧，超出则放左侧
val rawX = tooltipDpX + 12.dp
val finalX = if (rawX + tooltipMaxWidth > parentWidth) {
    maxOf(4.dp, tooltipDpX - tooltipMaxWidth - 12.dp)  // 翻到左侧
} else {
    maxOf(4.dp, rawX)
}

// 优先放点上方，超出则放下方
val rawY = tooltipDpY - 10.dp
val finalY = if (rawY < 4.dp) {
    minOf(tooltipDpY + 10.dp, parentHeight - 200.dp - 4.dp)  // 翻到下方
} else {
    maxOf(4.dp, rawY)
}
```

**文件：** `ChartTooltip.kt`, `HistoryScreen.kt`

---

## 问题 5：祈福输入支持多行，不限行数

### 修复
`PrayerScreen.kt` 第 172 行：
- 移除 `singleLine = true`
- 不设 `maxLines`
- `imeAction` 改为 `Default`

**文件：** `PrayerScreen.kt`

---

## 问题 6：自定义阶位

### 所有硬编码点

| 文件 | 内容 |
|------|------|
| `Rank.kt:11-22` | `RANKS` 列表 |
| `RankCalculator.kt:7-9` | `getRank()` 用 `Rank.RANKS` |
| `KarmaRepository.kt:170-183` | `getDecayRank()` — 独立硬编码 `when` |
| `KarmaRepository.kt:207-209` | `getRank()` 用 `Rank.RANKS` |
| `HistoryChartCanvas.kt:144` | `Rank.RANKS` 画色带 |
| `AxisCanvas.kt:87` | `Rank.RANKS` 画色带 |
| `SettingsScreen.kt:303,704` | `rankNames` 硬编码 |
| `MainViewModel.kt:74` | `repository.getRank()` |
| `PrayerViewModel.kt:32` | `repository.getRank()` |
| `Header.kt:61-64` | `rank.colorHex` / `rank.name` |

### 修复

1. **KarmaSettingsEntity** — 新增：
   - `rankThresholds: List<Float>`（8 个，默认 10,30,60,100,150,210,280,360）
   - `rankNames: List<String>`（9 个，默认壹~玖阶）
2. **KarmaDatabase** — Migration
3. **Rank.kt** — 移除 companion object，新增 `fromSettings()` 工厂方法
4. **RankCalculator.kt** — `getRank(score, ranks)` 接受参数
5. **KarmaRepository.kt** — `getRank()` 动态构建；`getDecayRank()` 改用 `rankThresholds`
6. **ViewModel 层** — 传入动态 ranks
7. **Canvas 层**（HistoryChartCanvas / AxisCanvas）— `Rank.RANKS` → 外部传入
8. **SettingsScreen.kt** — 两处 `rankNames` 改为 `draft.rankNames`；新增阈值编辑 UI
9. **SettingsViewModel.kt** — 新增 `updateRankThreshold()` / `updateRankName()`

**文件（约 12 个）**

---

## 问题 7：善果前缀 + 按类自定义输入 + 跟随滚动

### 原则
- 选善果确认 → 事件名拼 `"善果：xxx"`，不新开变量
- 三类各一个自定义输入框，普通事件高度的 ~1.2 倍
- 不限行数
- 在滚动区内跟随滚动

### 修复

**A. MainViewModel.kt `onConfirm()`：**
```kotlin
fun onConfirm() {
    val score = _customScore.value ?: _selectedScore.value ?: return
    val rawEvent = _customEvent.value ?: _selectedEvent.value ?: return
    // 如果事件来自善果预设，加"善果："前缀
    val isGoodResult = _selectedEvent.value in (goodResultPresets)
    val event = if (isGoodResult) "善果：$rawEvent" else rawEvent
    // ... 存储并重置
}
```
`goodResultPresets` 从 settings Flow 获取（`onConfirm` 时临时读取 cached settings）。

**B. EventPanel.kt — 三类自定义输入框：**
- 移除底部单一输入框
- 每个 `EventSection` 内末尾加一个 `OutlinedTextField`
- 默认高度 `minLines = 1`，不设 `maxLines`，自由扩展
- 初始最小高度参考普通事件项：当前 `.defaultMinSize(minHeight = 13.dp)`，输入框设为约 16dp
- `textStyle` 用 12sp，贴合事件项风格
- 占位文字如"自定义善业..."/"自定义恶业..."/"自定义善果..."
- 三个输入框都在 `verticalScroll` 区域内

**C. MainViewModel.kt — 三个独立 custom event 状态：**
```kotlin
private val _customGoodDeedEvent = MutableStateFlow<String?>(null)
private val _customBadDeedEvent = MutableStateFlow<String?>(null)
private val _customGoodResultEvent = MutableStateFlow<String?>(null)
```

有效事件优先级：对应分类的自定义 > 对应分类的预设。确认时取三者中有值的那个。

**D. MainScreen.kt — 更新 EventPanel 调用。**

**文件：** `MainViewModel.kt`, `EventPanel.kt`, `MainScreen.kt`

---

## 验证

1. 返回防抖 ✓
2. 周范围 ✓
3. 缩小按钮 ✓
4. Tooltip 智能定位 + 不限行换行 ✓
5. 祈福不限行 ✓
6. 自定义阶位全局生效 ✓
7. 善果前缀 + 三分类自定义输入 + 跟随滚动 ✓

---

## 实施顺序

1 → 2 → 3 → 4 → 5 → 7 → 6（按复杂度递增）
