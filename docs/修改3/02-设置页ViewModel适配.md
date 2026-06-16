# 步骤 02：设置页 ViewModel 适配

## 目标

在 `SettingsViewModel` 中替换旧的 `updateEventPresets` 方法为三个独立更新方法。

## 文件

`ui/settings/SettingsViewModel.kt`

## 改动

### 删除
```kotlin
fun updateEventPresets(lines: String) {
    val events = lines.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    setDraft(_draft.value.copy(eventPresets = events))
}
```

### 新增三个方法

```kotlin
fun updateGoodDeedPresets(lines: String) {
    val events = lines.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    setDraft(_draft.value.copy(goodDeedPresets = events))
}

fun updateBadDeedPresets(lines: String) {
    val events = lines.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    setDraft(_draft.value.copy(badDeedPresets = events))
}

fun updateGoodResultPresets(lines: String) {
    val events = lines.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    setDraft(_draft.value.copy(goodResultPresets = events))
}
```

## 注意事项

- 三个方法的逻辑完全相同，只是更新的字段不同
- `setDraft` 标记用户已编辑（`_userEdited = true`），防止异步加载覆盖
- 保存时通过 `repository.updateAllSettings(_draft.value)` 一次性持久化所有字段
