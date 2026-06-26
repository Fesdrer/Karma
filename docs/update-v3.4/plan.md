# 计时功能实现计划

## Context

用户需要在 Karma 应用主页面添加计时功能：选择分数和事件后，按计时按钮开始正计时，进入新页面显示时间，支持暂停/停止。熄屏后继续计时，停止时按公式 `round((秒数/60/15) * 分数 * 2) / 2` 计算 delta 并写入历史记录。

## 计时按钮放置方案

**放在三栏内容区和 Footer 之间**，作为独立的全宽操作按钮行，而不是塞进 Footer。

理由：
- Footer 已有 5 个按钮（确认/祈福/占卜/历史/⚙），再加一个会过于拥挤
- 计时按钮是"操作按钮"而非"导航按钮"，语义上应与 Footer 分离
- 全宽按钮更醒目，视觉上形成 "选择分数 → 选择事件 → 开始计时" 的自然流程

布局变为：
```
Header（分数 + 排名）
三栏（分数轴 | 仪表 | 事件）
计时按钮行（▶ 开始计时）        ← 新增
Footer（确认/祈福/占卜/历史/⚙）
```

启用条件：已选择分数 AND 已选择事件（与确认按钮相同）。点击后导航到计时页面。

## 实现步骤

### 1. 新增 Screen 路由

**文件**: `app/src/main/java/com/example/karma/ui/navigation/Screen.kt`

添加 `data object Timer : Screen("timer")`。

### 2. 创建 TimerService（前台服务，保证熄屏计时）

**新文件**: `app/src/main/java/com/example/karma/ui/timer/TimerService.kt`

- 前台 Service，启动时创建通知渠道 `karma_timer` 并显示通知
- 使用 `SystemClock.elapsedRealtime()` 记录开始时间（不受系统时间调整影响）
- 内部维护计时状态：`RUNNING` / `PAUSED` / `STOPPED`
- 暂停时记录已累计时间，恢复时从中断点继续
- 通过 `MutableStateFlow<TimerState>` 暴露当前状态给 UI
- Companion object 提供 `timerState` 静态 Flow，Compose 层可直接 collect
- 通知显示当前计时（HH:MM:SS），点击通知可返回计时页面

**计入暂停时间的算法**：
```
startElapsed = SystemClock.elapsedRealtime()  // 开始时记录
accumulatedMs = 0  // 暂停期间累计的已计时毫秒数

// 运行时：
elapsedMs = accumulatedMs + (now - resumeElapsed)  // resumeElapsed 是最近一次恢复时的 elapsedRealtime

// 暂停时：
accumulatedMs = elapsedMs（冻结当前值）
```

### 3. 创建 TimerScreen（计时页面 UI）

**新文件**: `app/src/main/java/com/example/karma/ui/timer/TimerScreen.kt`

- 全屏暗色背景，竖排布局：
  - **中央大字号时间**：HH:MM:SS，使用 `displayLarge` 或更大字号，白色/金色
  - 显示所选事件名（小字，灰色）
  - 显示所选分数（小字，灰色）
  - **底部两个按钮**：
    - 暂停（运行时）/ 继续（暂停时）— 蓝色
    - 停止 — 红色
- 使用 `LaunchedEffect` + `delay(200ms)` 循环更新显示时间
- 时间来源：从 `TimerService.timerState` collect，计算 `elapsedRealtime - startElapsed + accumulatedMs`
- 停止时：
  1. 计算 delta = `round((totalSeconds / 60.0 / 15.0) * selectedScore * 2) / 2`
  2. 调用 `repository.addHistoryEntry(delta, event, "record")`
  3. 停止 TimerService
  4. 导航回主页面

### 4. 创建 TimerViewModel（计时逻辑）

**新文件**: `app/src/main/java/com/example/karma/ui/timer/TimerViewModel.kt`

- 持有 `selectedScore: Float` 和 `selectedEvent: String`（从导航参数传入）
- 持有 `repository: KarmaRepository` 引用
- `onStop(elapsedSeconds: Long)`: 计算 delta 并写入数据库
- 格式化时间方法：`formatTime(seconds: Long): String` → "HH:MM:SS"

### 5. 修改 MainScreen — 添加计时按钮

**文件**: `app/src/main/java/com/example/karma/ui/main/MainScreen.kt`

在 `Row`（三栏）和 `Spacer`（Footer 上方）之间插入计时按钮行：
```kotlin
// 计时按钮
TimerButton(
    enabled = state.selectedScore != null && state.selectedEvent != null,
    selectedScore = state.selectedScore,
    selectedEvent = state.selectedEvent,
    onClick = { onNavigateToTimer(state.selectedScore!!, state.selectedEvent!!) },
)
```

### 6. 创建 TimerButton 组件

**新文件**: `app/src/main/java/com/example/karma/ui/main/components/TimerButton.kt`

- 全宽按钮，圆角，暗色背景
- 启用时：显示 "▶ 开始计时" + 所选分数和事件摘要
- 禁用时：灰色，显示 "请选择分数和事件"
- 样式匹配 Footer 的设计风格（`Color(0xFF16213e)` 背景等）

### 7. 修改 MainScreen 的导航签名

**文件**: `app/src/main/java/com/example/karma/ui/main/MainScreen.kt`

添加参数：`onNavigateToTimer: (score: Float, event: String) -> Unit`

### 8. 修改 NavGraph — 注册新路由

**文件**: `app/src/main/java/com/example/karma/ui/navigation/NavGraph.kt`

添加 Timer 路由，传递 `score` 和 `event` 参数：
```kotlin
composable("timer/{score}/{event}") { backStackEntry ->
    val score = backStackEntry.arguments?.getString("score")?.toFloatOrNull() ?: return@composable
    val event = backStackEntry.arguments?.getString("event") ?: return@composable
    TimerScreen(
        score = score,
        event = event,
        appContainer = appContainer,
        onBack = { navController.popBackStack() },
    )
}
```

### 9. 修改 AndroidManifest — 注册 Service + 通知权限

**文件**: `app/src/main/AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- 在 application 内 -->
<service
    android:name=".ui.timer.TimerService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />
```

### 10. 请求通知权限（运行时）

在 TimerScreen 启动时检查 `POST_NOTIFICATIONS` 权限（Android 13+），若无则请求。此权限是前台 Service 正常运行的前提。

### 11. 修改 build.gradle.kts（如需要）

确认 `targetSdk = 34` 时前台 Service 需要声明 `foregroundServiceType`，已在 Manifest 中处理。

## 不修改的内容

- **数据库 schema 不变**：计时产生的记录使用现有的 `HistoryEntryEntity`（type="record"），不需要新增字段
- **现有 Footer 不变**：计时按钮不在 Footer 内
- **确认按钮保留**：用户仍可以不用计时直接记录

## 文件变更清单

| 操作 | 文件 |
|------|------|
| 新增 | `ui/timer/TimerService.kt` |
| 新增 | `ui/timer/TimerScreen.kt` |
| 新增 | `ui/timer/TimerViewModel.kt` |
| 新增 | `ui/main/components/TimerButton.kt` |
| 修改 | `ui/navigation/Screen.kt` |
| 修改 | `ui/navigation/NavGraph.kt` |
| 修改 | `ui/main/MainScreen.kt` |
| 修改 | `AndroidManifest.xml` |

## 验证方式

1. 编译：`./gradlew assembleDebug` 确认无编译错误
2. 手动测试流程：
   - 打开应用，在主页选择分数（点击刻度）+ 选择事件
   - 点击 "开始计时" 按钮 → 进入计时页面，时间从 00:00:00 开始递增
   - 点击暂停 → 时间暂停，按钮变为 "继续"
   - 点击继续 → 时间继续走
   - 熄屏等待 30 秒 → 看到通知栏计时更新
   - 亮屏 → 时间正确（包括熄屏期间的时间）
   - 点击停止 → 返回主页，历史记录中出现新条目（计算公式验证）
