# Karma v3.2 — 第三轮修复计划

## Context

第二轮修复（A-F）完成后的 4 个遗留问题。

---

## 问题 1：占卜页面返回 → 只剩背景色

### 根因
`DivinationScreen.kt:47` — `.clickable { onBack() }` 无防抖，与 History 同样的问题。快速连点导致多次 `popBackStack()`。

### 修复
加防抖标记，与 HistoryScreen 完全一致的方案：
```kotlin
var backHandled by remember { mutableStateOf(false) }
.clickable {
    if (!backHandled) {
        backHandled = true
        onBack()
    }
}
```

**文件：** `DivinationScreen.kt`

---

## 问题 2：自定义事件输入框光标偏上 + 输入后上下间距变窄

### 根因
`EventPanel.kt:183-220` — 当前用 `Box` + `heightIn(min = 16.dp)` + `contentAlignment = CenterStart` 包裹 `BasicTextField`：

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
        .clip(...)
        .background(...)
        .border(...)
        .padding(horizontal = 8.dp)
        .heightIn(min = 16.dp),       // ← 问题：最小高度 + 居中
    contentAlignment = Alignment.CenterStart,  // ← 问题：垂直居中使光标悬浮
) {
    BasicTextField(...)
}
```

问题分析：
1. **光标偏上**：`contentAlignment = CenterStart` 在空态时把 `BasicTextField`（自然高度约 12sp ≈ 16dp）在 16dp 的 Box 中垂直居中。但 `BasicTextField` 的 cursor 基于文本行高计算，居中时与视觉中心有偏差，显得偏上。
2. **输入后变窄**：文本输入后 `BasicTextField` 内联高度增长，Box 随之扩展超出 `min=16dp`。但 Box 的 `CenterStart` 对齐不再变化，内容贴紧 Box 内边界，视觉上顶部间距变小（之前有居中带来的空白，现在没了）。

### 修复
去掉 `heightIn(min = ...)` 和 `contentAlignment`，改用固定 `padding` 让间距始终一致：

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 1.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(ScoreBtnBg)
        .border(1.dp, titleColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 5.dp),  // 固定内边距，始终一致
) {
    BasicTextField(
        value = customText,
        onValueChange = { ... },
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = titleColor.copy(alpha = 0.8f),
            fontSize = 12.sp,
        ),
        singleLine = false,
        decorationBox = { innerTextField ->
            Box {
                if (customText.isEmpty()) {
                    Text(customPlaceholder, fontSize = 11.sp, color = Color(0xFF666666))
                }
                innerTextField()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
```

关键变更：
- 移除 `.heightIn(min = 16.dp)` 和 `contentAlignment = Alignment.CenterStart`
- 统一用 `.padding(horizontal = 8.dp, vertical = 5.dp)` 固定内边距
- `BasicTextField` 自然靠内容撑高，不加最小高度约束

**文件：** `EventPanel.kt`

---

## 问题 3：祈福动画括号独立包裹文本

### 当前状态
`ParticleEngineCanvas.kt:272` 和 `301` — 括号嵌入文本字符串：`"「$purpose」"`，作为内容的一部分居中。
```
「第一行文字
第二行文字」
```

### 期望效果
括号独立绘制在文本块的对角，包裹整个文本：
```
「  第一行文字
      第二行文字  」
```

### 修复
1. 将 StaticLayout 的文本从 `"「$purpose」"` 改为 `purpose`（不含括号）
2. 绘制完 `layout.draw()` 后，分别用 `drawText` 在文本块对角落画括号：
   - `「` 在文本块的左上角（内缩几个 dp）
   - `」` 在文本块的右下角（内缩几个 dp）

以 normal 模式为例：
```kotlin
// 1. 构建 purpose 文本的 StaticLayout（不含括号）
val innerMaxWidth = (w * 0.7f).toInt()  // 略缩，给括号留空间
val innerLayout = android.text.StaticLayout.Builder
    .obtain(purpose, 0, purpose.length, normalTextPaint, innerMaxWidth)
    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
    .build()

// 2. 绘制 purpose 文本
drawContext.canvas.save()
val layoutX = cx - innerMaxWidth / 2f
val layoutY = cy + 45f * scale
drawContext.canvas.translate(layoutX, layoutY)
innerLayout.draw(drawContext.canvas.nativeCanvas)

// 3. 在文本块左上角画「
drawContext.canvas.nativeCanvas.drawText(
    "「",
    layoutX + 4.dp.toPx(),                         // 左边界内缩
    layoutY + normalTextPaint.textSize,             // 第一行基线
    bracketPaint
)

// 4. 在文本块右下角画」
val lastLineBottom = innerLayout.height
val lastLineWidth = innerLayout.getLineWidth(innerLayout.lineCount - 1)
val rightEdge = layoutX + innerMaxWidth
drawContext.canvas.nativeCanvas.drawText(
    "」",
    rightEdge - normalTextPaint.measureText("」") - 4.dp.toPx(),  // 右边界内缩
    layoutY + lastLineBottom,                                     // 最后行底部
    bracketPaint
)

drawContext.canvas.restore()
```

注意：需获取 density 用于 dp→px 转换。Compose 中通过 `density` 属性获取：
```kotlin
val density = this  // DrawScope 继承自 Density
val bracketPadding = 8.sp.toPx()  // 使用 sp/dp 转 px
```

两个模式（divine / normal）都需要改，括号颜色与对应文本 paint 一致。

**文件：** `ParticleEngineCanvas.kt`

---

## 问题 4：周标签年份写完整

### 当前状态
`HistoryViewModel.kt:250-253` — `% 100` 取后两位：
```kotlin
val monYear = monCal.get(Calendar.YEAR) % 100
val sunYear = sunCal.get(Calendar.YEAR) % 100
```
显示为 `"25/6/7-25/6/13"`

### 修复
去掉 `% 100`，用完整四位年份：
```kotlin
val monYear = monCal.get(Calendar.YEAR)
val sunYear = sunCal.get(Calendar.YEAR)
```
显示为 `"2025/6/7-2025/6/13"`

**文件：** `HistoryViewModel.kt`

---

## 涉及文件汇总

| 问题 | 文件 | 改动量 |
|------|------|--------|
| 1. 占卜返回防抖 | `DivinationScreen.kt` | +3 行 |
| 2. 输入框修复 | `EventPanel.kt` | 改 Box modifier |
| 3. 括号独立包裹 | `ParticleEngineCanvas.kt` | 重写 purpose 绘制逻辑（两处） |
| 4. 年份写完整 | `HistoryViewModel.kt` | 删 `% 100` |

---

## 验证

1. 占卜页快速连点返回 → 只退出一次
2. 自定义输入框光标垂直居中，输入文字后上下间距不变
3. 祈福动画括号位于文本块左上/右下对角，不贴在文字上
4. 周标签显示 `"2025/6/7-2025/6/13"`
