# Karma v3.2 — 第二轮修复计划

## Context

基于第一轮修复（1-5,7）后发现的 6 个遗留/新问题。所有代码均位于 `app/src/main/java/com/example/karma/` 下。

---

## 问题 A：三个自定义事件输入框高度太大

### 当前状态
`EventPanel.kt:206-208` — `OutlinedTextField` 用 `defaultMinSize(minHeight = 16.dp)`，加上前方 `Spacer(height=2.dp)`，加上 OutlinedTextField 自带的内部 padding，视觉高度远超事件项（`minHeight = 13.dp`）的 1.2 倍。

### 修复
1. 移除 `Spacer(Modifier.height(2.dp))`（第 184 行）
2. `defaultMinSize(minHeight = 16.dp)` → `heightIn(min = 13.dp)`，让初始高度更紧凑
3. 减少 OutlinedTextField 内部 padding：添加 `.padding(0.dp)` 和 `contentPadding` 控制
4. `textStyle` 保持 12sp，`minLines = 1`

**文件：** `EventPanel.kt`

---

## 问题 B：祈福界面括号位置不对

### 当前状态
`PrayerScreen.kt:150-190` — 「和」放在 `Row` 中，左右对称、垂直居中。用户要求「在左上角、」在右下角。

### 修复
将 `Row` 改为 `Box`，用 `Modifier.align()` 把括号定位到对角：
```kotlin
Box(modifier = ...) {
    Text("「", modifier = Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = 4.dp), ...)
    OutlinedTextField(value = ..., modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), ...)
    Text("」", modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 4.dp), ...)
}
```
OutlinedTextField 左右留 20dp 给括号让位。

**文件：** `PrayerScreen.kt`

---

## 问题 C：祈福动画页面多行文字不换行

### 当前状态
`ParticleEngineCanvas.kt:271-280` 和 `294-303` — 用 `nativeCanvas.drawText()` 绘制 `"「$purpose」"`，这是 Android Canvas 的单行文本 API，不支持 `\n` 换行。如果 `purpose` 含多行，会被挤成一行或出现乱码。

### 修复
使用 `android.text.StaticLayout` 代替 `nativeCanvas.drawText()`，支持自动换行和多行绘制：
```kotlin
val textPaint = android.text.TextPaint().apply {
    color = ...
    textSize = ...
    isFakeBoldText = true
    textAlign = android.graphics.Paint.Align.CENTER
}
val layout = android.text.StaticLayout.Builder
    .obtain("「$purpose」", 0, purpose.length + 2, textPaint, (w * 0.8f).toInt())
    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
    .build()
canvas.save()
canvas.translate(cx - layout.width / 2f, cy + offsetY)
layout.draw(canvas)
canvas.restore()
```
宽度限制为画布宽度的 80%，防止太长超出屏幕。

在两处绘制 `purpose` 文本的地方（divine 模式约第 271 行，normal 模式约第 294 行）都需要修改。

**文件：** `ParticleEngineCanvas.kt`

---

## 问题 D：Tooltip 需要判断四个方向的溢出

### 当前状态
`HistoryScreen.kt:427-445` — Tooltip 定位只判断右侧溢出（翻到左侧）和上方溢出（翻到下方），但：
- **左侧**：只用 `maxOf(4.dp, ...)` 夹持，不会翻到右侧
- **下方**：只用 `minOf(..., parentHeight - tooltipMaxHeight - 4.dp)` 夹持，不会翻到上方

如果数据点靠近左上角 → tooltip 向右上方展开仍可能溢出左侧和上方。

### 修复
完善四方向翻转逻辑：
```kotlin
// X 方向：优先右侧，溢出则左侧
val rawX = tooltipDpX + 12.dp
val finalX = if (rawX + tooltipMaxWidth > parentWidth) {
    // 溢出右侧 → 放左侧
    maxOf(4.dp, tooltipDpX - tooltipMaxWidth - 12.dp)
} else if (rawX < 4.dp) {
    // 溢出左侧 → 放右侧（很少发生，但防御）
    minOf(tooltipDpX + 12.dp, parentWidth - tooltipMaxWidth - 4.dp)
} else {
    rawX
}

// Y 方向：优先上方，溢出则下方
val rawY = tooltipDpY - tooltipMaxHeight - 10.dp
val finalY = if (rawY < 4.dp) {
    // 溢出上方 → 放下方
    minOf(tooltipDpY + 10.dp, parentHeight - tooltipMaxHeight - 4.dp)
} else if (rawY + tooltipMaxHeight > parentHeight) {
    // 溢出下方 → 放上方
    maxOf(4.dp, tooltipDpY - tooltipMaxHeight - 10.dp)
} else {
    rawY
}
```

**文件：** `HistoryScreen.kt`

---

## 问题 E：导出下拉框移到视图模式下拉框右边

### 当前状态
`HistoryScreen.kt` — 视图模式下拉框在 Row 1 右侧，导出下拉框在 Row 2 右侧。两个下拉框各占一行右端。

### 修复
1. **Row 1**：Back + Title 在左；视图模式下拉 + 导出下拉并排在右（中间加点间距）
2. **Row 2**：只保留左侧导航控件，去掉右侧导出下拉
3. 将导出下拉相关的 `var exportExpanded` 和整个 `Box { ... DropdownMenu }` 从 Row 2 移到 Row 1，放在视图模式下拉框右侧

具体布局：
```
Row 1: [← 返回] [历史记录] ——— [日 ▼] [导出 ▼]
Row 2: [<] [dateLabel] [>] [今天] [放大] [缩小]
```

**文件：** `HistoryScreen.kt`

---

## 问题 F：周显示格式加年份

### 当前状态
`HistoryViewModel.kt:250` — 周标签格式为 `"M/d-M/d"`，如 `"6/7-6/13"`。缺少年份信息，跨年周容易混淆。

### 修复
改为 `"yy/M/d-yy/M/d"` 格式：
```kotlin
val monYear = monCal.get(Calendar.YEAR) % 100
val sunYear = sunCal.get(Calendar.YEAR) % 100
"${monYear}/${monCal.get(Calendar.MONTH) + 1}/${monCal.get(Calendar.DAY_OF_MONTH)}-${sunYear}/${sunCal.get(Calendar.MONTH) + 1}/${sunCal.get(Calendar.DAY_OF_MONTH)}"
```
效果：`"25/6/7-25/6/13"`，跨年时如 `"25/12/29-26/1/4"`。

**文件：** `HistoryViewModel.kt`

---

## 涉及文件汇总

| 问题 | 文件 |
|------|------|
| A. 输入框高度 | `EventPanel.kt` |
| B. 括号对角 | `PrayerScreen.kt` |
| C. 动画多行 | `ParticleEngineCanvas.kt` |
| D. Tooltip 四向 | `HistoryScreen.kt` |
| E. 导出下拉移位 | `HistoryScreen.kt` |
| F. 周加年份 | `HistoryViewModel.kt` |

共 5 个文件。

---

## 验证

1. 自定义输入框只有事件项的 1.2 倍高，无多余间距
2. 祈福括号「在左上角、」在右下角，输入框撑满中间
3. 祈福多行文字在动画中正确分行显示
4. Tooltip 四个方向均能正确翻转，始终在屏幕内
5. 导出下拉和视图下拉在同一行右侧
6. 周标签显示 `"25/6/7-25/6/13"` 格式
