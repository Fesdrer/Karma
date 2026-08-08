# 任务4：Apple 设计原则打磨 + 设置页过渡卡顿修复

## 1. 起因与过程

1. 通过 `/apple-design` skill，按 Apple 流体交互原则（响应性、直接操作、弹簧动画、材质化、空间一致性、成就感时刻）审查并调整整个应用。调整完成、编译通过。
2. 用户反馈：**打开和关闭设置页变得很卡**（此前设置页是瞬间切换）。
3. 排查确认卡顿机制后，依次尝试「单向渐变」→「延迟补全卡片」，最终**设置页恢复瞬间进入 + 瞬间离开**（用户决定）。其他页面过渡不受影响。

最终状态：apple-design 的打磨全部保留；设置页过渡相关改动**全部回退**（NavGraph/SettingsScreen 与任务前逐字一致，仅 NavGraph 保留一段解释性注释）。

## 2. Apple 设计原则调整（生效中）

### 2.1 按压反馈 `ui/components/PressFeedback.kt`（新增）

```kotlin
@Composable
fun Modifier.pressFeedback(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.95f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        if (pressed) {
            scale.animateTo(pressedScale, animationSpec = tween(90))
        } else {
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium))
        }
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
```

要点（因果链）：
- 按下 90ms 快速缩到 0.95，松开用弹簧回弹（damping 0.9，可中断——快速连按不跳变）。
- 只用 `graphicsLayer` 改 transform，不触发布局。
- 搭配 `clickable(interactionSource, indication = null)` 使用（深色主题下 ripple 几乎不可见，用缩放替代）。

接入位置：Footer 六个分区、TimerButton、BackButton、EventPanel（开始/暂停/停止按钮 + 事件项）。

### 2.2 Header 功德分数弹跳（成就感时刻）

`ui/main/components/Header.kt`：总分变化时数字瞬间放大到 1.2，spring（damping 0.6）回弹，轻微过冲表达「加功」的庆祝时刻。首次组合只记录不弹（避免导航返回误触发）。

### 2.3 分数轴拖动 1:1 跟手（直接操作）

`ui/main/components/ScorePanel.kt`：拖动中实时 snap 会让数值在 .5 刻度间跳动、与手指脱节。改为拖动全程跟随手指原始位置（`onScoreSelected(score)`），释放（`onDragEnd`/`onDragCancel`）时才吸附到最近半格。点按逻辑不变（直接吸附）。

### 2.4 弹窗材质化入场（materialize, don't just fade）

`ui/components/DialogEntrance.kt`（新增）：遮罩淡入 200ms + 卡片从 0.92 弹簧放大（damping 0.8，轻微过冲，读起来像「真实材质到达」）。`maskAlpha = 0f` 时无遮罩（纯内容入场）。

接入位置：誓约新建/详情弹窗（`ui/bet/BetScreen.kt`）、祈福表单（`ui/prayer/PrayerScreen.kt`）。

### 2.5 导航过渡一致性

`ui/navigation/NavGraph.kt`：设置页从 `tween(0)` 改为与其余子页一致的 250ms 淡入淡出。**此改动因卡顿已回退**（见第 3 节）。

## 3. 设置页过渡卡顿：机制、尝试、最终方案

### 3.1 现象

设置页改为 250ms 淡入淡出后，打开和关闭都明显卡顿，渐变看起来「缓慢」且一顿一顿。回退为瞬间切换后流畅。

### 3.2 机制（因果链，重要——避免下次重蹈覆辙）

1. **静止页面零成本**：屏幕上没有变化时，UI 线程不产生新帧，渲染线程什么都不做，直接复用上一帧结果。这就是平时流畅的原因。
2. **渐变强制每帧重绘**：alpha 每帧都在变 → 每帧都要产生新帧 → 每帧从头执行一次完整绘制通道（遍历整棵绘制树 + 裁剪测试 + 光栅化可见部分）。
3. **导航淡入淡出是交叉淡化**：进入时 Main 淡出 + Settings 淡入**同时**进行，两页同屏，每帧要把两个页面的完整绘制各执行一遍。
4. **设置页是全应用最重的页面**：`SettingsScreen.kt` 用 `Column + verticalScroll` **一次性组合全部卡片**（约 10 张卡片、十几个 Slider、十几个 OutlinedTextField，几百个绘制节点）。屏幕外的卡片不会被光栅化，但**每帧遍历+裁剪测试它们仍要付成本**（树在，遍历就在）。
5. **叠加全屏星星位图**：`Theme.kt` 的 `theme_bg`（FillBounds 大图）每帧绘制。
6. 结论：Main + Settings 两棵树的完整绘制远超 16.6ms 帧预算 → 掉帧 → 渐变被拉长、看起来「缓慢」。

为什么其他页面（历史/祈福/誓约/占卜）一直 250ms 不卡：它们轻，Main + 它们挤得进一帧预算；唯独 Main + 设置挤不进。

### 3.3 尝试过的方案与结论

| 方案 | 做法 | 结果 |
|---|---|---|
| ① 交叉淡化 250ms | 四方向全部 250ms | ❌ 卡顿（用户反馈） |
| ② 单向渐变 | 进入：Main `tween(0)` 立即消失、Settings 淡入；退出：Main `tween(0)` 立即出现、Settings 淡出。每帧只重绘一棵树 | ❌ 仍卡（一棵完整 Settings 树也超预算） |
| ③ 延迟补全卡片 | 进入时树里只放「分数区域 + 刻度区域」两张卡片，其余九张在按下按钮后 250ms（曾用 300ms）再进入树；退出改瞬时 | ⚠️ 方案可行但用户放弃（页面先短后长有观感问题，且退出无法用同招——退出时是完整树） |
| ④ 瞬间切换（最终） | 设置页四方向全部 `tween(0)` | ✅ 流畅，用户接受 |

**尝试③的完整思路**（若未来想复用）：`LaunchedEffect(Unit) { delay(250); showRest = true }`——计时从按下按钮（页面首次组合）开始，与淡入 0~250ms 同步，补全卡片出现在屏幕下方视口外（用户看不见）。注意：退出方向无法用这招（退出时用户可能滚到任意位置，树必须是完整的）。

### 3.4 最终方案（生效中）

`ui/navigation/NavGraph.kt` 设置页：

```kotlin
// 设置页是全应用最重的页面，任何方向的淡入淡出都会让每帧重绘超预算掉帧，
// 最终方案：瞬间进入 + 瞬间离开。
enterTransition = { fadeIn(tween(0)) },
exitTransition = { fadeOut(tween(0)) },
popEnterTransition = { fadeIn(tween(0)) },
popExitTransition = { fadeOut(tween(0)) },
```

`SettingsScreen.kt` 恢复「一次性组合全部卡片」，无净改动。

### 3.5 未来可用的方案（未实施，留档）

如果以后想给设置页加过渡又不卡：**离屏图层（"拍照"方案）**——`Modifier.graphicsLayer(alpha = 动画值, compositingStrategy = CompositingStrategy.Offscreen)`，子树先渲染进离屏缓冲，动画期间每帧只做一次纹理混合，成本与树的大小无关。障碍：NavHost 的过渡动画无法注入 offscreen 合成，需要把渐变搬到页面自己手里（自带动画 + BackHandler 拦截返回键），代码量较大。

## 4. 修改文件清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `ui/components/PressFeedback.kt` | 新增 | 按压反馈 modifier |
| `ui/components/DialogEntrance.kt` | 新增 | 弹窗分层入场容器 |
| `ui/main/components/Footer.kt` | 修改 | 六分区接入按压反馈 |
| `ui/main/components/TimerButton.kt` | 修改 | 接入按压反馈 |
| `ui/components/BackButton.kt` | 修改 | 接入按压反馈 |
| `ui/main/components/EventPanel.kt` | 修改 | 开始/暂停/停止按钮 + 事件项接入按压反馈 |
| `ui/main/components/Header.kt` | 修改 | 分数变化 spring 弹跳 |
| `ui/main/components/ScorePanel.kt` | 修改 | 拖动 1:1 跟手、释放吸附半格 |
| `ui/bet/BetScreen.kt` | 修改 | 两个弹窗用 DialogEntranceContainer |
| `ui/prayer/PrayerScreen.kt` | 修改 | 祈福表单用 DialogEntranceContainer(maskAlpha = 0f) |
| `ui/navigation/NavGraph.kt` | 修改（净改动=注释） | 设置页过渡反复试验后回退 tween(0)，保留一段解释注释 |
| `ui/settings/SettingsScreen.kt` | 修改（净改动=无） | 曾加延迟补全机制，已删除，恢复原状 |

不改：MainViewModel、TimerService、数据层、主题。

## 5. 验证清单

1. `compileDebugKotlin` EXIT=0（多次验证）。
2. 按压底部栏/计时按钮/返回/事件项 → 有 0.95 缩放 + 松开弹簧回弹。
3. 确认功德后 Header 分数弹跳一次；导航返回不误触发。
4. 分数轴拖动全程跟手，松手吸附最近半格；点按直接吸附。
5. 誓约新建/详情弹窗、祈福表单：遮罩淡入 + 卡片弹簧放大入场。
6. 设置页瞬间进入、瞬间离开，**无卡顿**。
7. 其他页面（历史/祈福/誓约/占卜）250ms 淡入淡出不变。
