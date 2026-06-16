# 步骤 04：MainViewModel 适配新字段

## 目标

将 `MainUiState` 中的 `eventPresets` 替换为三个独立列表，并在 `uiState` 组合中从 settings 读取新字段。

## 文件

`ui/main/MainViewModel.kt`

## 改动

### 4.1 修改 MainUiState

```kotlin
data class MainUiState(
    val totalScore: Float = 0f,
    val rank: Rank? = null,
    val scorePresets: List<Float> = emptyList(),
    // ★ 删除：val eventPresets: List<String> = emptyList(),
    // ★ 新增三个字段：
    val goodDeedPresets: List<String> = emptyList(),
    val badDeedPresets: List<String> = emptyList(),
    val goodResultPresets: List<String> = emptyList(),
    val selectedScore: Float? = null,
    val selectedEvent: String? = null,
    // ... 以下所有字段保持不变 ...
)
```

### 4.2 修改 uiState 的 combine lambda

在 `uiState` 的 `combine` lambda 中（约第 62-90 行），找到：

```kotlin
eventPresets = settings.eventPresets,
```

替换为：

```kotlin
goodDeedPresets = settings.goodDeedPresets,
badDeedPresets = settings.badDeedPresets,
goodResultPresets = settings.goodResultPresets,
```

> 注意：`settings` 的类型是 `KarmaSettingsEntity`，刚才我们在步骤 01 中给它加了三个新字段。

### 4.3 没有其他改动

- `selectEvent()`、`onCustomEventChanged()`、`onConfirm()` 方法无需改动，因为它们操作的是 `_selectedEvent` / `_customEvent` 状态，与事件列表的具体分类无关
- `_effectiveEvent` 的 combine 逻辑也无需改动

## 解释

MainViewModel 只负责透传事件列表数据到 UI，不关心列表的内部结构（是扁平还是三段）。三段式的渲染逻辑完全在 EventPanel 中实现。
