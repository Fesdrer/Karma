# 阶段 4：三柱六宫 Canvas — XiaoLiuRenPillarCanvas.kt

## 目标

创建 `XiaoLiuRenPillarCanvas.kt`，用 Compose Canvas 绘制三根代表手指的金色光柱，每柱分上下两段对应六宫。

## 文件路径

```
app/src/main/java/com/example/karma/ui/divination/components/XiaoLiuRenPillarCanvas.kt
```

---

## 详细设计

### 组件签名

```kotlin
@Composable
fun XiaoLiuRenPillarCanvas(
    highlightedIndex: Int?,      // 当前高亮的宫索引（金线所在位置）
    resultIndex: Int?,           // 最终结果宫索引（动画完成后强烈发光）
    onPalacePositionsReady: (List<PalacePosition>) -> Unit,  // 回调：六宫的屏幕坐标
    modifier: Modifier = Modifier,
)
```

### PalacePosition 数据结构

```kotlin
data class PalacePosition(
    val index: Int,           // 宫索引 0-5
    val centerX: Float,       // 该宫中心的 X 坐标
    val centerY: Float,       // 该宫中心的 Y 坐标
    val bounds: Rect,         // 该宫的边界矩形（用于金线连接点计算）
)
```

### 柱子几何布局

```
屏幕坐标系（Canvas 内，单位 px）：

         留连(P1)      速喜(P2)      赤口(P3)
       ┌────────┐   ┌────────┐   ┌────────┐
       │ 上段    │   │ 上段    │   │ 上段    │  ← 每段高约 screenH*0.2
       ├─分隔环─┤   ├─分隔环─┤   ├─分隔环─┤
       │ 下段    │   │ 下段    │   │ 下段    │  ← 每段高约 screenH*0.2
       └────────┘   └────────┘   └────────┘
         大安(P0)      空亡(P5)      小吉(P4)

   ← 左柱(食指) → ← 中柱(中指) → ← 右柱(无名指) →

柱子布局参数：
- 总宽度 = screenW * 0.78（三柱 + 两间距）
- 柱宽 = 总宽度 / 3 * 0.75
- 柱间距 = 总宽度 / 3 * 0.25
- 柱高 = 上下段各 screenH * 0.2，总高 screenH * 0.4 + 分隔
- 整体居中，中柱略微上移（弧形排列）
```

### 坐标映射

六宫与 Canvas 坐标的对应关系：

| 宫索引 | 宫名 | 柱子 | 段 | 坐标位置 |
|:---:|------|------|----|---------|
| 0 | 大安 | 左柱 | 下段 | `(leftX, bottomY)` |
| 1 | 留连 | 左柱 | 上段 | `(leftX, topY)` |
| 2 | 速喜 | 中柱 | 上段 | `(centerX, topY - arcOffset)` |
| 3 | 赤口 | 右柱 | 上段 | `(rightX, topY)` |
| 4 | 小吉 | 右柱 | 下段 | `(rightX, bottomY)` |
| 5 | 空亡 | 中柱 | 下段 | `(centerX, bottomY + arcOffset)` |

> 弧形排列：中柱上移 `arcOffset = 20.dp.toPx()`，营造拱形排列感

### 绘制步骤（在 DrawScope 内）

#### Step 1：计算柱位坐标

```kotlin
val screenW = size.width
val screenH = size.height
val totalWidth = screenW * 0.78f
val pillarWidth = totalWidth / 3f * 0.75f
val gapWidth = totalWidth / 3f * 0.25f
val startX = (screenW - totalWidth) / 2f
val segmentHeight = screenH * 0.18f
val separatorHeight = 12.dp.toPx()
val totalPillarHeight = segmentHeight * 2f + separatorHeight
val baseY = (screenH - totalPillarHeight) / 2f
val arcOffset = 20.dp.toPx()

// 三柱 X 坐标
val leftX = startX + pillarWidth / 2f
val centerX = startX + pillarWidth + gapWidth + pillarWidth / 2f
val rightX = startX + (pillarWidth + gapWidth) * 2f + pillarWidth / 2f

// 六宫中心坐标
val positions = listOf(
    PalacePosition(0, leftX,  baseY + segmentHeight * 1.5f + separatorHeight, ...), // 大安（左下）
    PalacePosition(1, leftX,  baseY + segmentHeight * 0.5f, ...),                     // 留连（左上）
    PalacePosition(2, centerX, baseY + segmentHeight * 0.5f - arcOffset, ...),        // 速喜（中上）
    PalacePosition(3, rightX, baseY + segmentHeight * 0.5f, ...),                     // 赤口（右上）
    PalacePosition(4, rightX, baseY + segmentHeight * 1.5f + separatorHeight, ...),   // 小吉（右下）
    PalacePosition(5, centerX, baseY + segmentHeight * 1.5f + separatorHeight + arcOffset, ...), // 空亡（中下）
)
```

#### Step 2：绘制背景微粒子

```kotlin
// 极淡的金色漂浮粒子（仿香火烟气）
// 约 15-20 个粒子，缓慢上升，周期性循环
for (particle in backgroundParticles) {
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = particle.alpha * 0.12f),
        radius = particle.radius,
        center = Offset(particle.x, particle.y),
    )
}
```

粒子动画：使用 `rememberInfiniteTransition` + `animateFloat` 控制每个粒子的 Y 偏移和 alpha。

#### Step 3：绘制三柱（每柱包含上段 + 分隔环 + 下段）

```kotlin
fun DrawScope.drawPillar(centerX: Float, baseY: Float, segmentHeight: Float, ...) {
    val pillarWidth = totalWidth / 3f * 0.75f
    val halfW = pillarWidth / 2f
    val cornerRadius = 16.dp.toPx()

    // 上段
    drawPillarSegment(
        rect = Rect(centerX - halfW, baseY, centerX + halfW, baseY + segmentHeight),
        label = "留连",     // 具体标签由外部传入
        sixGods = "玄武",
        highlightAlpha = ...,
    )

    // 分隔环
    val sepY = baseY + segmentHeight
    drawLine(                                        // 金色横线
        color = Color(0xFFFFD700).copy(alpha = 0.7f),
        start = Offset(centerX - halfW * 0.8f, sepY + separatorHeight / 2f),
        end = Offset(centerX + halfW * 0.8f, sepY + separatorHeight / 2f),
        strokeWidth = 2.dp.toPx(),
    )
    drawCircle(                                      // 中心装饰圆点
        color = Color(0xFFFFD700),
        radius = 3.dp.toPx(),
        center = Offset(centerX, sepY + separatorHeight / 2f),
    )

    // 下段
    drawPillarSegment(
        rect = Rect(centerX - halfW, baseY + segmentHeight + separatorHeight,
                    centerX + halfW, baseY + segmentHeight * 2f + separatorHeight),
        label = "大安",
        sixGods = "青龙",
        highlightAlpha = ...,
    )
}
```

#### Step 4：绘制单段柱体

```kotlin
fun DrawScope.drawPillarSegment(
    rect: Rect,
    label: String,
    sixGods: String,
    highlightAlpha: Float,
) {
    val cornerRadius = 16.dp.toPx()

    // 1. 呼吸光晕（外层发光）
    val breathAlpha = (sin(currentTime * 0.004f) * 0.5f + 0.5f) * 0.15f + highlightAlpha * 0.3f
    drawRoundRect(
        color = Color(0xFFFFD700).copy(alpha = breathAlpha),
        topLeft = Offset(rect.left - 8.dp.toPx(), rect.top - 8.dp.toPx()),
        size = Size(rect.width + 16.dp.toPx(), rect.height + 16.dp.toPx()),
        cornerRadius = CornerRadius(cornerRadius + 8.dp.toPx()),
    )

    // 2. 柱身填充（暗金色半透明）
    drawRoundRect(
        color = Color(0xFF3d2a00).copy(alpha = 0.5f),  // 暗金填充
        cornerRadius = CornerRadius(cornerRadius),
        ...)

    // 3. 金色边框（高亮时变亮）
    val borderColor = if (highlightAlpha > 0.5f) {
        Color(0xFFFFF8DC).copy(alpha = 0.9f)  // 亮白金色
    } else {
        Color(0xFFFFD700).copy(alpha = 0.6f)  // 正常金色
    }
    drawRoundRect(
        color = borderColor,
        style = Stroke(width = 2.dp.toPx()),
        cornerRadius = CornerRadius(cornerRadius),
        ...)

    // 4. 顶部装饰线（微微上翘如飞檐）
    val brimWidth = rect.width * 0.85f
    val brimY = rect.top + 6.dp.toPx()
    drawLine(Color(0xFFFFD700).copy(alpha = 0.5f),
        Offset(rect.center.x - brimWidth/2, brimY + 3.dp.toPx()),
        Offset(rect.center.x + brimWidth/2, brimY + 3.dp.toPx()),
        strokeWidth = 1.5f)
    drawLine(Color(0xFFFFD700).copy(alpha = 0.7f),
        Offset(rect.center.x - brimWidth/2 + 4.dp.toPx(), brimY),
        Offset(rect.center.x + brimWidth/2 - 4.dp.toPx(), brimY),
        strokeWidth = 1.5f)

    // 5. 宫名标签
    drawContext.canvas.nativeCanvas.drawText(
        label,
        rect.center.x,
        rect.center.y + 4.dp.toPx(),
        android.graphics.Paint().apply {
            color = 0xFFdaa520.toInt()  // 古铜金色 Goldenrod
            textSize = 18.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        },
    )

    // 6. 六神小标签（角落）
    drawContext.canvas.nativeCanvas.drawText(
        sixGods,
        rect.center.x,
        rect.center.y + 24.dp.toPx(),
        android.graphics.Paint().apply {
            color = 0x88ffd700.toInt()
            textSize = 11.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        },
    )
}
```

#### Step 5：高亮效果叠加

当 `highlightedIndex` 或 `resultIndex` 匹配当前宫时：
- 边框颜色 → 亮白金色
- 呼吸光晕 alpha +0.3
- `resultIndex` 匹配时：额外绘制一个脉冲金光环（`drawCircle` + 周期性半径和 alpha）

---

## 呼吸光晕时间驱动

```kotlin
val breathTime = remember { mutableFloatStateOf(0f) }

LaunchedEffect(Unit) {
    while (true) {
        withFrameMillis { frameTime ->
            breathTime.floatValue = frameTime / 1000f
        }
    }
}
```

> 使用 `withFrameMillis` 而非 `rememberInfiniteTransition`，与粒子动画共享时间基准。

---

## 性能考虑

- 柱子几何数据在首次 layout 时计算并缓存（`remember { derivedStateOf {} }`）
- 背景粒子限制在 20 个以内，每个粒子只存储 position + alpha + radius
- 不使用 bitmap 操作，全部矢量绘制
- 仅在 `highlightedIndex` / `resultIndex` 变化时触发重组

---

## 验证检查点

- [ ] 三柱正确居中显示
- [ ] 上下段标签位置正确（大安下/留连上/速喜上/空亡下/赤口上/小吉下）
- [ ] 呼吸光晕平滑动画
- [ ] 高亮状态边框变亮
- [ ] `palacePositions` 坐标回调正确传给父组件
- [ ] 不同屏幕尺寸下柱子成比例缩放

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
