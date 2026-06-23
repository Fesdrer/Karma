# 自定义事件输入框根因分析与修复

## 问题描述

1. **光标偏上**：输入框中闪烁竖线不在方框垂直中间
2. **输入后方框上下间距变窄**：无文字时有正常间距，输入多行后间距消失、文字紧贴边框

## 根因分析

### 问题 1：光标偏上

当前代码用 `Box(padding(vertical = 5.dp))` 包裹 `BasicTextField`：

```
┌── Box ──────────────┐
│  5dp padding        │  ← 空白
│  ┌─ BasicTextField ┐│
│  │ cursor █        ││  ← cursor 从 BasicTextField 顶部开始
│  │ text area       ││
│  └─────────────────┘│
│  5dp padding        │  ← 空白
└─────────────────────┘
```

`BasicTextField` 内部无上边距，cursor 从字段虚拟区域的最顶端（y=0）开始。Box 的 `padding(vertical = 5.dp)` 只在外面包了一层空白，但没有让 cursor 在**文本行内**居中。

`lineHeight` 未显式设置时，12sp 字体默认行高约 18-20px（含 `includeFontPadding`），cursor 顶部在字体 ascent 处而非对齐到 padding 的中线。导致 cursor 整体偏上。

### 问题 2：间距变窄

固定 `padding(vertical = 5.dp)` 在空态时占比大（5dp / 总高度 ≈ 20%），输入多行后总高度增长但 padding 绝对值不变（5dp / 总高度 ≈ 5%），视觉上"变窄"。

根本原因：**外层 padding 不能随内容等比缩放**。

## 修复方案

核心思路：**不用 Box padding 做间距，回归 `Box(heightIn(min=))` + `contentAlignment = Center` + `BasicTextField 自带 vertical padding`。**

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 1.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(ScoreBtnBg)
        .border(1.dp, titleColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
        .heightIn(min = 18.dp),       // 事件项 13dp × 1.2 ≈ 16dp，取 18dp 保证最小视觉
    contentAlignment = Alignment.Center,  // 垂直居中，空态时 BasicTextField 在 Box 中间
) {
    BasicTextField(
        value = customText,
        onValueChange = {
            customText = it
            onCustomChanged(it)
        },
        textStyle = TextStyle(
            color = titleColor.copy(alpha = 0.8f),
            fontSize = 12.sp,
            lineHeight = 16.sp,                                 // 显式行高
            platformStyle = PlatformTextStyle(includeFontPadding = false),  // 去掉额外字间距
        ),
        singleLine = false,
        decorationBox = { innerTextField ->
            Box {
                if (customText.isEmpty()) {
                    Text(
                        customPlaceholder,
                        fontSize = 11.sp,
                        color = Color(0xFF666666),
                    )
                }
                innerTextField()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),  // BasicTextField 自带 padding
    )
}
```

### 为什么这样能解决

**光标居中**：

```
Box: heightIn(min = 18.dp), contentAlignment = Center

空态：
┌── Box (18dp) ────────────────────────┐
│                                       │  ← Center 产生的上下边距（各 ~1dp）
│  ┌─ BasicTextField (padding 4dp) ──┐ │
│  │  4dp 上边距（BasicTextField 自身）│ │
│  │  █ cursor（16sp = 约16dp）      │ │  ← 光标准确在 16dp 行高中心
│  │  4dp 下边距                      │ │
│  └──────────────────────────────────┘ │
└───────────────────────────────────────┘
```

- `lineHeight = 16.sp` 固定行高，cursor 从 line 顶部画到底部，cursor 中心 = 8sp 处，不受 font padding 干扰
- `includeFontPadding = false` 去掉 Android 默认的上下额外空白
- `contentAlignment = Center` 把 BasicTextField 摆在 Box 正中

**间距不变**：

```
输入多行后：
┌── Box (自动撑高) ──────────────────────┐
│  ┌─ BasicTextField (padding 4dp) ──┐  │
│  │  4dp 上边距（始终不变）          │  │
│  │  第一行文字                      │  │
│  │  第二行文字                      │  │
│  │  4dp 下边距（始终不变）          │  │
│  └──────────────────────────────────┘  │
└───────────────────────────────────────┘
```

- `BasicTextField` 自带 `padding(vertical = 4.dp)`，与行数无关，始终提供一致的呼吸空间
- Box 随内容自动扩展，`heightIn(min = 18.dp)` 只在空态起作用

### 需要新增的 import
```kotlin
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
```

## 涉及文件

`EventPanel.kt` — 替换第 183-219 行的自定义输入 Box + BasicTextField 块。

## 验证

1. 空态光标竖线出现在输入框垂直正中
2. 输入单行文字后上下间距与空态一致
3. 输入多行文字后间距依然不变
4. 输入框整体高度约为普通事件项的 1.2 倍
