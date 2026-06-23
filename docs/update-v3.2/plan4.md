# Karma v3.2 — 可变阶位完整重构计划

## 审计结果

经过全代码库扫描，在 **8 个文件** 中发现 **11 处** 9 阶硬编码假设：

| 位置 | 硬编码内容 | 严重程度 |
|------|-----------|----------|
| `KarmaRepository.kt:167-183` | `getDecayRank()` 硬编码 8 个阈值边界 | ✅ 已修复（前轮改为动态） |
| `Rank.kt:11-22` | `RANKS` 9 个固定元素 | ✅ 已修复（改为 `listFrom()`） |
| `HistoryChartCanvas.kt:144` | `Rank.RANKS` 引用 | ✅ 已修复（改为动态 `ranks`） |
| `AxisCanvas.kt:87` | `Rank.RANKS` 引用 | ✅ 已修复（改为动态 `ranks`） |
| `SettingsScreen.kt:303/704` | 硬编码 `rankNames` 列表 ×2 处 | ✅ 已修复 |
| **`SettingsScreen.kt`** | **3×3 网格 + 5行2列衰减 + 阈值4行2列 + 名称编辑** | ❌ 待重构 |
| **`ParticleEngineCanvas.kt:97-98`** | **`rankLevel >= 9` / `<= 3` / `<= 6` 固定边界** | ❌ 待修改 |
| **`Header.kt:61`** | **`rank.level <= 6` 文字颜色边界** | ❌ 待修改 |
| `Color.kt:42-53` | 9 个命名颜色常量 | ⚠️ 仅默认值，保留 |
| `KarmaSettingsEntity.kt` | 默认 9 个颜色/名称/衰减/8 个阈值 | ⚠️ 仅默认值，保留 |
| `KarmaDatabase.kt` | Migration 默认 9 元素 JSON | ⚠️ 仅初始值，保留 |

---

## 修改方案

### 一、SettingsScreen.kt — 重做阶位设置 UI（最大改动）

**移除**：
- `AxisSettingsCard` 中的：阶位颜色 3×3 网格 + 阶位阈值编辑 + 阶位名称编辑
- `DecaySettingsCard` 中的：5 行各阶位衰减扣除量
- `RankDecayItem` 和 `RankDecayItem` 调用

**新增** `RankSettingsCard` 独立卡片：

```
┌─ 阶位设置 ──────────────────────────┐
│                                      │
│  ┌─ 壹阶 ──────────────────── [×] ┐ │
│  │  [■]  上限：[ 10 ]            │ │
│  │  业力衰减：[-] [2] [+]       │ │
│  └────────────────────────────────┘ │
│  ...（N 个阶位）                     │
│  ┌─ 玖阶 ──────────────────── [×] ┐ │  (最后一阶上限 ∞，不可编辑)
│  │  [■]  上限： ∞                │ │
│  │  业力衰减：[-] [3] [+]       │ │
│  └────────────────────────────────┘ │
│                          [+]  [-]   │
└──────────────────────────────────────┘
```

每个 `RankItem(row, index)` 包含：
- **名称** — `OutlinedTextField`，值绑定 `draft.rankNames[index]`
- **颜色** — `ColorSwatch`，点击打开 `ColorPickerDialog`（照搬现有逻辑）
- **上限** — `OutlinedTextField`（数字键盘），仅前 N-1 个可编辑，最后一个显示 `"∞"` 且不可编辑
- **衰减** — `[-]` 按钮 + 数值显示 + `[+]` 按钮（照搬现有 RankDecayItem 逻辑）
- **删除按钮** — `[×]`，仅当 `deleteMode = true` 时显示，红色圆圈白色叉号

底部按钮行：
- `[+]` — 始终可点击，添加一个阶位（复制最后一个的属性作为默认）
- `[-]` — 切换 `deleteMode`。亮起（参考历史"放大"激活样式）= 删除模式激活；灰色 = 关闭

行为约束：
- 最少 1 个阶位
- 当只剩 1 阶位时，`[-]` 自动熄灭、`deleteMode` 自动关闭、× 消失
- `deleteMode` 切换逻辑：`deleteMode = !deleteMode && rankCount > 1`

**SettingsViewModel 新增**：
```kotlin
private val _deleteMode = MutableStateFlow(false)
val deleteMode: StateFlow<Boolean> = _deleteMode.asStateFlow()

fun addRank() {
    val draft = _draft.value
    val newNames = draft.rankNames + "新阶位"
    val newColors = draft.rankColors + (draft.rankColors.lastOrNull() ?: 0xFFFFFFFF)
    val newThresholds = draft.rankThresholds + (draft.rankThresholds.lastOrNull()?.plus(50f) ?: 360f)
    val newDecays = draft.rankDecayAmounts + (draft.rankDecayAmounts.lastOrNull() ?: 3f)
    setDraft(draft.copy(
        rankNames = newNames,
        rankColors = newColors,
        rankThresholds = newThresholds,
        rankDecayAmounts = newDecays,
    ))
}

fun deleteRank(index: Int) {
    val draft = _draft.value
    if (draft.rankNames.size <= 1) return
    val newNames = draft.rankNames.toMutableList().apply { removeAt(index) }
    val newColors = draft.rankColors.toMutableList().apply { removeAt(index) }
    val newDecays = draft.rankDecayAmounts.toMutableList().apply { removeAt(index) }
    // 删除对应阈值：第 i 阶的阈值是 thresholds[i-1]（上限）和 thresholds[i]（下限）
    // 实际：thresholds 是 N-1 个，第 k 个是阶位 k 的上限
    val newThresholds = draft.rankThresholds.toMutableList()
    if (index < newThresholds.size) newThresholds.removeAt(index)
    else if (newThresholds.isNotEmpty()) newThresholds.removeAt(newThresholds.lastIndex)
    setDraft(draft.copy(
        rankNames = newNames,
        rankColors = newColors,
        rankThresholds = newThresholds,
        rankDecayAmounts = newDecays,
    ))
    if (newNames.size <= 1) _deleteMode.value = false
}

fun toggleDeleteMode() {
    if (_draft.value.rankNames.size > 1) {
        _deleteMode.value = !_deleteMode.value
    } else {
        _deleteMode.value = false
    }
}
```

**注意**：阶位名称编辑不能再用批量 TextField → 改为每个阶位单独 `OutlinedTextField`（`singleLine = true`），避免批量更新循环问题。

---

### 二、ParticleEngineCanvas.kt — 动态粒子等级

**当前**（第 97-98 行）：
```kotlin
tier = TIERS[(rankLevel >= 9).let { if (it) 3 else if (rankLevel <= 3) 0 else if (rankLevel <= 6) 1 else 2 }]
isDivine = rankLevel >= 9
```

**修改**：传入 `totalRanks: Int` 参数，改为相对计算：
```kotlin
val ratio = rankLevel.toFloat() / totalRanks.toFloat()
tier = when {
    rankLevel >= totalRanks -> TIERS[3]  // 最高阶 = divine
    ratio <= 0.33f -> TIERS[0]
    ratio <= 0.66f -> TIERS[1]
    else -> TIERS[2]
}
isDivine = rankLevel >= totalRanks
```

**影响**：`ParticleEngineCanvas` 签名增加 `totalRanks: Int` 参数，调用者（`PrayerScreen.kt`）需传入。

**PrayerViewModel.kt**：`PrayerUiState` 新增 `totalRanks: Int = 9`，init 中设置。

---

### 三、Header.kt — 动态文字颜色

**当前**（第 61 行）：
```kotlin
val textColor = if (rank.level <= 6) Color.White else Color(0xFFffd700)
```

**修改**：Header 接收 `ranks: List<Rank>` 参数，改为相对计算：
```kotlin
val totalRanks = ranks.size
val textColor = if (totalRanks > 0 && rank.level <= totalRanks * 2 / 3) 
    Color.White else Color(0xFFffd700)
```

**影响**：`Header` 签名增加 `ranks: List<Rank>` 参数，`MainScreen.kt` 调用时传入 `state.ranks`。

---

### 四、已有基础（无需变更）

以下已在前面轮次中动态化，无需改动：
- `Rank.listFrom(thresholds, names, colors)` — 自动适配任意数量
- `KarmaRepository.getRank(score, settings)` — 动态
- `KarmaRepository.getDecayRank(score, thresholds)` — 动态
- `HistoryChartCanvas` / `AxisCanvas` — 接收动态 `ranks`
- `MainViewModel` / `HistoryViewModel` — combine 中动态构建 ranks
- `HistoryUiState` / `MainUiState` — 已有 `ranks: List<Rank>`

---

## 涉及文件汇总

| 文件 | 改动 |
|------|------|
| `SettingsScreen.kt` | 重做：删分散 UI，新增 `RankSettingsCard` + `RankItem` + +/-按钮 + 删除模式 |
| `SettingsViewModel.kt` | 新增：`addRank()` / `deleteRank()` / `toggleDeleteMode()` / `deleteMode` state |
| `ParticleEngineCanvas.kt` | 新增 `totalRanks` 参数，相对化粒子等级计算 |
| `PrayerViewModel.kt` | `PrayerUiState` 新增 `totalRanks`，从 settings 计算 |
| `PrayerScreen.kt` | 传递 `totalRanks` 给 `ParticleEngineCanvas` |
| `Header.kt` | 新增 `ranks` 参数，相对化文字颜色阈值 |
| `MainScreen.kt` | 传递 `state.ranks` 给 `Header` |

共 **7 个文件**。

---

## 验证

1. 设置页"阶位设置"卡片含 N 个阶位行，每行名称/颜色/上限/衰减聚集
2. [+] 添加阶位，[-] 切换删除模式（亮起）
3. 删除模式下 × 显示，点击 × 删除对应阶位
4. 只剩 1 阶位时 [-] 自动熄灭、× 消失
5. 修改后保存按钮亮起，主屏 Header + 轴 + 历史色带跟随
6. 粒子动画等级随可变阶位数自适应
7. Header 阶位名颜色随总阶位数自适应
