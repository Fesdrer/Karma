# Plan: Fix BasicTextField Cursor Offset & Padding Shrinking — V2

## Context

EventPanel.kt 中三个分类的自定义输入框使用 `BasicTextField`，存在两个视觉问题：
1. 光标偏上
2. 输入后上下间距变窄

第一版修复（添加 `LineHeightStyle.Alignment.Center`、移动 padding 到容器、统一 fontSize）**未生效**。

## Why V1 Failed — Root Cause Deep Dive

关键发现：**placeholder 和 text field 使用了不同的文本度量（text metrics）**，即使在上一版修复后仍然如此。

| 属性 | Placeholder Text | BasicTextField textStyle |
|---|---|---|
| fontSize | 12.sp ✅ | 12.sp ✅ |
| lineHeight | **默认（约 14-15sp）** | **16.sp 显式** |
| includeFontPadding | **默认 true** | **false** |
| lineHeightStyle | 无 | Alignment.Center |

这两个 Text 在空/非空状态下产生不同的高度，导致：
- **空状态**：decorationBox 按 placeholder 度量计算高度（默认 font padding，默认 lineHeight）
- **有文字**：decorationBox 按 innerTextField 度量计算高度（includeFontPadding=false, lineHeight=16.sp）
- **切换时高度突变** → 视觉间距变化

**光标偏上**：`includeFontPadding = false` 改变了 Android `StaticLayout` 中文本的基线位置，使文本（和光标）在可用空间内上移。`LineHeightStyle.Alignment.Center` 未能纠正此位移（可能是 Huawei EMUI 设备上的 Compose 实现差异）。

## Solution V2：统一使用 Android 默认文本度量

核心思路：**完全移除所有 TextStyle 覆写，使用与上方预设事件项一致的默认 Android 文本行为。**

上方预设事件项（行 172-182）只用 `fontSize = 12.sp`，没有 `lineHeight`、`includeFontPadding`、`lineHeightStyle` 等覆写。自定义输入框应与此保持一致。

### 具体修改（仅修改 EventSection 函数，~第 30、186-228 行）

#### 1. 移除不再需要的 import

```diff
- import androidx.compose.ui.text.PlatformTextStyle
- import androidx.compose.ui.text.style.LineHeightStyle
```

#### 2. 重构自定义输入框代码

将当前代码（第 186-228 行）替换为：

```kotlin
        // Custom event input — compact, ~1.2x event item height
        val fieldTextStyle = TextStyle(
            color = titleColor.copy(alpha = 0.8f),
            fontSize = 12.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ScoreBtnBg)
                .border(1.dp, titleColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            BasicTextField(
                value = customText,
                onValueChange = {
                    customText = it
                    onCustomChanged(it)
                },
                textStyle = fieldTextStyle,
                singleLine = false,
                cursorBrush = SolidColor(titleColor),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (customText.isEmpty()) {
                            Text(
                                text = customPlaceholder,
                                style = fieldTextStyle.copy(color = Color(0xFF666666)),
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
```

#### 关键变更：

| # | 变更 | 原理 |
|---|---|---|
| 1 | **移除 `lineHeight = 16.sp`** | 与预设事件项一致，使用默认行高 |
| 2 | **移除 `platformStyle = PlatformTextStyle(includeFontPadding = false)`** | 恢复 Android 默认字体 padding，光标自然居中 |
| 3 | **移除 `lineHeightStyle = LineHeightStyle(...)`** | 无需手动控制行内对齐 |
| 4 | **placeholder 使用 `style = fieldTextStyle.copy(color = ...)`** | placeholder 和 text field 使用完全相同度量，彻底消除高度差异 |
| 5 | **提取 `fieldTextStyle` 为变量** | 确保样式只定义一次，placeholder 和 field 共享 |
| 6 | **添加 `cursorBrush = SolidColor(titleColor)`** | 确保光标颜色与语义颜色一致 |

### 需要新增的 import

```kotlin
import androidx.compose.ui.graphics.SolidColor
```

## 为什么这次能成功

1. **placeholder 与 text field 共享完全相同的 TextStyle** — 空/非空状态 text metrics 完全一致，decorationBox 高度不会因输入而变化
2. **不覆写 `includeFontPadding`** — 使用 Android 默认行为（`true`），光标和文本由系统自然定位，不受 Huawei/EMUI 自定义文本引擎干扰
3. **不覆写 `lineHeight`** — 行高完全基于字体自然度量，与上方预设事件项视觉一致
4. **padding 在容器层** — 5dp 上下 padding 在 border 之后，间距恒定不受内容影响

## Verification

1. `./gradlew assembleDebug` 编译通过
2. 在设备上运行：空输入框光标应位于方框垂直中间
3. 输入文字：光标保持在中间，上下间距不变
4. 输入多行文字：文字不紧贴边框，上下间距与空状态一致
5. 三个分类（善业/恶业/善果）行为一致
6. 自定义输入框文字与上方预设事件项文字外观协调
