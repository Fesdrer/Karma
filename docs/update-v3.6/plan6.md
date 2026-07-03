# Karma App 性能优化 & 冗余代码清理计划

## Context

Karma 是一个 Kotlin/Jetpack Compose Android app（业力值管理），用户反馈「应用时长卡顿」。经过全量代码审查（所有 52 个 .kt 文件），发现以下问题分类：

1. **主线程阻塞** — 1000 次循环 + LuckAmplifier 重计算在主线程
2. **Canvas draw 中反复创建对象** — Paint/Calendar/TextMeasurer 每帧重建
3. **动画帧循环不 vsync** — `delay(16)` 轮询而非 `withFrameNanos`
4. **无限循环未绑定生命周期** — PillarCanvas 呼吸动画永不停止
5. **每帧大量内存分配** — removeAll、toList、toDouble boxing 引发 GC
6. **死代码 & 重复代码** — RankCalculator 未使用、多处复制粘贴
7. **硬编码颜色值** — 散落各处的 `0xFF1A1A1A` 等魔法数字

---

## P0 — 关键性能（直接导致 UI 卡顿）

### 1. [DivinationScreen.kt] 主线程 1000 次循环移到协程

**问题**：气运测试按钮 onClick 中 `for (i in 1..1000) { Random.nextInt(1, 1001) }` 在主线程执行，直接阻塞 UI。

**修改**：循环移到 `viewModelScope.launch(Dispatchers.Default)`，结果通过 `mutableStateOf` 回传。

### 2. [MainViewModel.kt] 将 LuckAmplifier 移出 combine 热路径

**问题**：`combine` lambda 内调用 `LuckAmplifier.computeLuckAmplification()` — erf/sqrt/exp/groupBy+sortedBy/逐段积分，每次 settings 或 history 流发射都在主线程执行。

**修改**：单独 `combine(history, settings).debounce(300)` → `MutableStateFlow<Float?>`，uiState 直接读缓存值。

### 3. [ParticleEngineCanvas.kt] Paint/TextPaint/StaticLayout 对象提升 + 减少分配

**问题**：每帧创建 6+ Paint、2 TextPaint、2 StaticLayout.Builder（文本布局计算极昂贵）。`sparks.removeAll{}` / `runes.removeAll{}` 分配新列表。28 处 `toDouble()`/`toFloat()` 装箱。

**修改**：
- Paint/TextPaint → `remember` 缓存
- StaticLayout 仅在 purpose 文本改变时重建（`remember(purpose)` 包裹）
- `removeAll{}` → `iterator.remove()` 原地删除
- 去重 divine/non-divine 文本绘制代码（当前 ~100 行重复）

### 4. [AxisCanvas.kt] Paint 对象提升到 remember

**问题**：4 处 `new android.graphics.Paint()` 在 Canvas draw 内每帧创建。

**修改**：Composable 体内 `remember { Paint() }` 创建，draw 内只设属性。

### 5. [HistoryChartCanvas.kt] Paint + Calendar 对象提升

**问题**：3 个 Paint（空数据/Y轴/X轴）+ `Calendar.getInstance()` 在 draw 循环中创建 6 次。Calendar 是 Android 中已知的昂贵调用。

**修改**：Paint + Calendar 用 `remember` 缓存，循环中复用同一 Calendar 实例。

### 6. [YarrowCanvas.kt] TextMeasurer.measure() 移出 Canvas draw

**问题**：`TextMeasurer.measure()` 在 Canvas draw 中多次调用。应预计算。

**修改**：Canvas 外用 `remember` + `derivedStateOf` 预测量，draw 内直接用 `TextLayoutResult.drawText()`。

### 7. [XiaoLiuRenPillarCanvas.kt] 无限呼吸动画添加生命周期感知

**问题**：`while(true) { delay(16); breathTime += 0.016f }` 永不停止，页面不可见时仍持续耗费 CPU。

**修改**：`LaunchedEffect` 绑定到组件可见性，离开时自动取消。

### 8. [XiaoLiuRenThreadCanvas.kt] 动画循环改用 withFrameNanos + 减少分配

**问题**：`delay(16)` 不与屏幕刷新率同步。`buffer.toList()` 每帧拷贝。`segments.count{}` 多次完整遍历。

**修改**：
- `while(!finished) { delay(16); ... }` → `withFrameNanos { frameNanos -> ... }`
- `toList()` 改为直接从 `ArrayDeque` 迭代
- `segments.count{}` 结果缓存为局部变量

---

## P1 — 高优先级（显著改善）

### 9. [RankCalculator.kt] 删除未使用文件

`RankCalculator.getRank()` 从未被调用。所有阶位查找通过 `KarmaRepository.getRank()` 完成。

→ **删除** `app/src/main/java/com/example/karma/util/RankCalculator.kt`

### 10. [ChartTooltip.kt] SimpleDateFormat 静态化

每次 Composable 调用都 `new SimpleDateFormat(...)`。

→ 提取为 companion object 常量。

### 11. [KarmaRepository.kt] 格式化日期代码去重

`formatDate()` 与 `HistoryViewModel.formatDateLabel()` / `SettingsViewModel` 中 Calendar→String 逻辑重复。

→ 在 Repository 中公开 `formatDate()` 或提取共享 companion 方法。

### 12. 返回按钮防抖模式去重

`HistoryScreen.kt` 和 `DivinationScreen.kt` 有完全相同模式的防抖返回按钮。

→ 提取为 `@Composable fun BackButton(onBack: () -> Unit)` 放在 `ui/components/`，两处调用。

---

## P2 — 中优先级（清理改善）

### 13. [MainViewModel.kt] 减少中间 StateFlow 链

`_effectiveScore` → `_effectiveEvent` → `_selectedPair` → 最终 combine，3 个中间 StateFlow 创建多余对象。

→ 在最终 combine 中直接计算 effectiveScore/effectiveEvent，删除中间流。

### 14. [SettingsScreen.kt] ColorPickerDialog 减少冗余 HSV→RGB 转换

3 个 `derivedStateOf` 各自独立调用 `Color.HSVToColor`，同一转换执行 3 次。

→ 合并为单个 `derivedStateOf` 返回 Triple(r, g, b)。

### 15. 硬编码颜色值统一使用主题常量

`0xFF1A1A1A`（面板背景）和 `0xFF334444`（边框色）在多个文件中硬编码，而 [Color.kt](app/src/main/java/com/example/karma/ui/theme/Color.kt) 已定义 `PanelBg`、`BorderSubtle` 等。

→ 搜索替换所有此类用法为对应主题常量。

**涉及文件**：HistoryScreen.kt, SettingsScreen.kt, PrayerScreen.kt, DivinationScreen.kt, ChartTooltip.kt 等。

---

## 修改文件清单

| 文件 | 改动类型 |
|---|---|
| [DivinationScreen.kt](app/src/main/java/com/example/karma/ui/divination/DivinationScreen.kt) | P0-1: 1000次循环移协程 + P1-12: 提取 BackButton |
| [MainViewModel.kt](app/src/main/java/com/example/karma/ui/main/MainViewModel.kt) | P0-2: LuckAmplifier 移出 combine + P2-13: 删除中间 StateFlow |
| [ParticleEngineCanvas.kt](app/src/main/java/com/example/karma/ui/prayer/ParticleEngineCanvas.kt) | P0-3: Paint 提升 + removeAll→iterator + 去重文本代码 |
| [AxisCanvas.kt](app/src/main/java/com/example/karma/ui/main/components/AxisCanvas.kt) | P0-4: Paint remember 化 |
| [HistoryChartCanvas.kt](app/src/main/java/com/example/karma/ui/history/HistoryChartCanvas.kt) | P0-5: Paint + Calendar remember 化 |
| [YarrowCanvas.kt](app/src/main/java/com/example/karma/ui/divination/components/YarrowCanvas.kt) | P0-6: TextMeasurer 预计算 |
| [XiaoLiuRenPillarCanvas.kt](app/src/main/java/com/example/karma/ui/divination/components/XiaoLiuRenPillarCanvas.kt) | P0-7: 无限循环加生命周期感知 |
| [XiaoLiuRenThreadCanvas.kt](app/src/main/java/com/example/karma/ui/divination/components/XiaoLiuRenThreadCanvas.kt) | P0-8: delay(16)→withFrameNanos + 减少分配 |
| [RankCalculator.kt](app/src/main/java/com/example/karma/util/RankCalculator.kt) | P1-9: **删除** |
| [ChartTooltip.kt](app/src/main/java/com/example/karma/ui/components/ChartTooltip.kt) | P1-10: SimpleDateFormat 静态化 |
| [KarmaRepository.kt](app/src/main/java/com/example/karma/data/repository/KarmaRepository.kt) | P1-11: formatDate 复用 |
| [HistoryScreen.kt](app/src/main/java/com/example/karma/ui/history/HistoryScreen.kt) | P1-12: 提取 BackButton + P2-15: 硬编码色替换 |
| [SettingsScreen.kt](app/src/main/java/com/example/karma/ui/settings/SettingsScreen.kt) | P2-14: 合并 HSV→RGB + P2-15: 硬编码色替换 |
| [PrayerScreen.kt](app/src/main/java/com/example/karma/ui/prayer/PrayerScreen.kt) | P2-15: 硬编码色替换 |
| 新建 [BackButton.kt](app/src/main/java/com/example/karma/ui/components/BackButton.kt) | P1-12: 共享防抖返回按钮 |

---

## 验证方式

1. **编译检查**：`./gradlew assembleDebug` 确保无编译错误
2. **功能验证**（安装到设备）：
   - 主屏幕：选择分数+事件 → 确认 → 检查总分更新流畅度
   - 运气增幅开关 → Header 中 luck 值正常显示
   - 历史页：切换日/周/月/全部 → 点击数据点 tooltip → 缩放/缩小
   - 祈福页：输入分数+目的 → 确认 → 粒子动画流畅不卡顿
   - 占卜页：小六壬完整动画流程 → 大衍筮法完整 18 变 → 查看启示
   - 气运测试：点击按钮不卡 UI
   - 设置页：修改各项参数 → 保存 → 返回主屏确认生效
   - 计时器：启动 → 暂停/继续 → 停止 → 记录写入
