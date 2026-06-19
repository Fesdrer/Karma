# 步骤 06：Footer 重写为分段式底部栏

## 目标

将原本的四个独立 Button 改为五个等宽 segment 的分段式底部长条布局（New Segmented Bottom Bar），新增"占卜"入口。

## 文件

`ui/main/components/Footer.kt`

## 新视觉设计

```
┌──────────────────────────────────────────────┐
│   确认   │   祈福   │   占卜   │   历史   │   设置   │
├──────────┼──────────┼──────────┼──────────┼──────────┤
│  蓝色填充  │  金色填充  │  灰色背景  │  灰色背景  │  灰色背景  │
└──────────────────────────────────────────────┘
```

- 每段等宽（`weight(1f)`）
- segment 之间用 1dp 竖线（Divider）分割
- 整体背景 `PanelBg`（`Color(0xFF16213e)`）
- "确认"和"祈福"使用填充样式，其余使用暗淡样式
- 禁用状态：整段变灰

## 新函数签名

```kotlin
@Composable
fun Footer(
    confirmEnabled: Boolean,
    prayerEnabled: Boolean,
    onConfirm: () -> Unit,
    onPrayer: () -> Unit,
    onDivination: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
)
```

## 完整实现

```kotlin
@Composable
fun Footer(
    confirmEnabled: Boolean,
    prayerEnabled: Boolean,
    onConfirm: () -> Unit,
    onPrayer: () -> Unit,
    onDivination: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segmentShape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(segmentShape)
            .background(Color(0xFF16213e)),
    ) {
        // 确认
        FooterSegment(
            text = "确认",
            enabled = confirmEnabled,
            activeColor = Color(0xFF4a90d9),
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 祈福
        FooterSegment(
            text = "祈福",
            enabled = prayerEnabled,
            activeColor = Color(0xFFb8860b),
            onClick = onPrayer,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 占卜 (NEW)
        FooterSegment(
            text = "占卜",
            enabled = true,
            activeColor = Color(0xFF555555),
            onClick = onDivination,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 历史
        FooterSegment(
            text = "历史",
            enabled = true,
            activeColor = Color(0xFF555555),
            onClick = onHistory,
            modifier = Modifier.weight(1f),
        )

        FooterDivider()

        // 设置
        FooterSegment(
            text = "⚙",
            enabled = true,
            activeColor = Color(0xFF555555),
            onClick = onSettings,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FooterSegment(
    text: String,
    enabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                if (enabled) activeColor.copy(alpha = 0.15f)
                else Color(0xFF222222)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> Color(0xFF555555)
                activeColor == Color(0xFF4a90d9) -> Color(0xFF4a90d9) // 确认蓝
                activeColor == Color(0xFFb8860b) -> Color(0xFFb8860b) // 祈福金
                else -> Color(0xFFa0c4ff) // 其他浅蓝
            },
        )
    }
}

@Composable
private fun FooterDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .align(Alignment.CenterVertically)
            .background(Color(0xFF334444)),
    )
}
```

## 需要新增的 import

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
```

不再需要的 import（可保留不会报错，但建议清理）：
- `BorderStroke`
- `Button`
- `ButtonDefaults`
- `OutlinedButton`

## 关键点

- 使用 `Row` 包裹五个 `FooterSegment`，整体用 `clip(RoundedCornerShape(10.dp))` 包裹外层
- `FooterDivider` 用 1dp 宽的竖线，高度 30dp
- "确认"和"祈福"使用各自的语义色（蓝色/金色）渲染文字和背景
- 其他三个 segment（占卜/历史/设置）使用统一的淡色样式
- `Box.clickable(enabled = enabled)` 控制可点击性，禁用时灰色
- 相比原版，不需要额外的 `Spacer` 做间距，直接紧邻排列
