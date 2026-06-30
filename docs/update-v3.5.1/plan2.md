# 修复：设置页面数字输入框无法清空的问题

## 背景

Karma Android 应用的设置页面（`SettingsScreen.kt`）中，6 个数字输入框（`OutlinedTextField`）存在相同的 Bug：**用户无法删掉最后一位数字**。当用户清空输入框时，文字会弹回之前的值，始终残留一个字符。

**根因**：每个输入框的 `value` 绑定到 `formatFloat(draft.某个Float值)`——一个从 ViewModel Float 状态派生出的字符串。当用户清空输入框时，`onValueChange` 收到 `""`，但 `"".toFloatOrNull()` 返回 `null`，`?.let {}` 块不执行，ViewModel 状态从未被更新，输入框重组时又显示旧值。空字符串被静默丢弃。

同一文件中已有正确的修复范例：`ColorTextField`（第 1008 行）使用了**本地 `String` 状态**（`var text by remember(value) { mutableStateOf(...) }`），在 `onValueChange` 中无条件更新本地文本，而只在解析成功时才回调父组件。该模式允许输入框显示空文本，同时保持父组件状态不变。

## 需修改的文件

- [SettingsScreen.kt](app/src/main/java/com/example/karma/ui/settings/SettingsScreen.kt) — 唯一需要修改的文件

## 6 个受影响的字段（均在 SettingsScreen.kt 中）

| # | 字段 | 行号 | 格式化方式 | 范围检查 |
|---|------|------|-----------|---------|
| 1 | `scoreAxisRangeMin` | 180–195 | `formatFloat()` | 无 |
| 2 | `scoreAxisRangeMax` | 199–215 | `formatFloat()` | 无 |
| 3 | `luckT` | 545–562 | `.toInt().toString()` | 1..365 |
| 4 | `luckB` | 569–586 | `formatFloat()` | 0.1..100 |
| 5 | `luckW` | 593–610 | `formatFloat()` | 1..1000 |
| 6 | 等级阈值 | 834–849 | `formatFloat()` | 无 |

## 修复模式（对每个字段应用相同修改）

为每个字段引入本地 `String` 状态来跟踪显示的文本，参照第 1008–1036 行已有的 `ColorTextField` 模式：

**修改前**（当前有问题的模式）：
```kotlin
OutlinedTextField(
    value = formatFloat(draft.scoreAxisRangeMin),          // 绑定到 Float 状态
    onValueChange = { v ->
        v.toFloatOrNull()?.let { viewModel.updateScoreAxisRangeMin(it) }  // 空字符串 → null → 不执行
    },
    ...
)
```

**修改后**（修复后的模式）：
```kotlin
var scoreAxisRangeMinText by remember(draft.scoreAxisRangeMin) {
    mutableStateOf(formatFloat(draft.scoreAxisRangeMin))
}

OutlinedTextField(
    value = scoreAxisRangeMinText,                        // 绑定到本地 String
    onValueChange = { v ->
        scoreAxisRangeMinText = v                         // 无条件更新显示
        v.toFloatOrNull()?.let { viewModel.updateScoreAxisRangeMin(it) }
    },
    ...
)
```

**要点**：
- `remember(draft.某个Float值)` 作为 key —— 当 ViewModel 值因外部原因变化时（如加载已保存的设置），重置本地文本。当用户自己的编辑更新了 Float 值时，key 改变，文本重新初始化为格式化后的版本。
- `scoreAxisRangeMinText = v` 无条件执行——空字符串被保存在本地状态中，输入框能正确显示为空。
- ViewModel 仅在解析成功时更新——保留现有的验证行为。

**luckT 特殊处理**：初始状态使用 `draft.luckT.toInt().toString()` 而非 `formatFloat()`（与当前显示格式保持一致）：
```kotlin
var luckTText by remember(draft.luckT) {
    mutableStateOf(draft.luckT.toInt().toString())
}
```
范围检查（`if (it >= 1f && it <= 365f)`）保持不变——仍然控制是否更新 ViewModel。

## 不做修改的部分

- `formatFloat()` 辅助函数（第 1194–1196 行）—— 无需修改
- `ColorTextField` 组件（第 1008–1036 行）—— 已正确实现，无需修改
- 其他所有页面（PrayerScreen、ScorePanel、ScoreEditModal、Divination）—— 均已正确处理清空逻辑，无需修改
- SettingsViewModel —— 无需修改
- KarmaSettingsEntity —— 无需修改

## 验证

1. **编译**：执行 `./gradlew assembleDebug` 确认编译通过。
2. **逐字段手动测试**：
   - 打开设置 → 分数设置卡片 → 点击"显示范围"最小值输入框 → 删除所有字符 → 输入框应保持为空。
   - 输入新数字 → 输入框应接受并显示。
   - 最大值输入框重复测试。
   - 打开设置 → 运气设置 → 开启运气功能 → 测试 T、b、W 三个字段——每个都应能完全清空。
   - 打开设置 → 等级设置 → 测试阈值字段——每个都应能完全清空。
3. **边界情况**：
   - 输入 `-`（负号）→ 输入框应显示 `-` 且不弹回（允许逐步输入负数，如 `-5`）。
   - 输入 `.`（小数点）→ 输入框应显示 `.` 且不弹回。
   - 输入超出范围的值（运气字段）→ 输入框应保持显示输入文本，但 ViewModel 不更新（保留现有范围限制行为）。
