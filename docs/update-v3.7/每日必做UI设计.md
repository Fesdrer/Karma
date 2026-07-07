# 每日必做功能 — 实现计划

## 需求概述

在设置页面中新增"每日必做"专区，让用户将善业标记为每日必做并设置未完成扣分。在主页面中，每日必做事件用 vis 变量（0=未做蓝色，1=已做绿色）区分颜色。

## 改动概览

共修改 **6 个文件**：

| 文件 | 改动 |
|------|------|
| `data/local/entity/KarmaSettingsEntity.kt` | +2 字段 |
| `data/local/KarmaDatabase.kt` | +Room 迁移 v10→v11 |
| `ui/settings/SettingsViewModel.kt` | +3 方法 |
| `ui/settings/SettingsScreen.kt` | +DailyMustDoCard |
| `ui/main/MainViewModel.kt` | +vis 追踪 + 确认时标记 |
| `ui/main/MainScreen.kt` | 传递 dailyMustDo 参数 |
| `ui/main/components/EventPanel.kt` | 善业颜色按 vis 变化 |

---

## 1. 数据层 — KarmaSettingsEntity

在 `KarmaSettingsEntity` 末尾新增两个字段：

```kotlin
// ===== 每日必做 =====
val dailyMustDoDeedNames: List<String> = emptyList(),
val dailyMustDoDeedPenalties: List<Float> = emptyList(),
```

Converters 已有 `List<String>` 和 `List<Float>` 的 TypeConverter，直接可用。

## 2. Room 迁移 v10 → v11

在 `KarmaDatabase.kt`：
- 数据库版本号：`version = 10` → `version = 11`
- 新增 `MIGRATION_10_11`：

```kotlin
private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoDeedNames TEXT NOT NULL DEFAULT '[]'")
        db.execSQL("ALTER TABLE karma_settings ADD COLUMN dailyMustDoDeedPenalties TEXT NOT NULL DEFAULT '[]'")
    }
}
```

- 在 `.addMigrations(...)` 链末尾追加 `MIGRATION_10_11`

## 3. SettingsViewModel — 新增方法

新增三个便捷方法：

```kotlin
fun toggleDailyMustDo(deedName: String, enabled: Boolean) {
    val currentNames = _draft.value.dailyMustDoDeedNames.toMutableList()
    val currentPenalties = _draft.value.dailyMustDoDeedPenalties.toMutableList()
    if (enabled) {
        // 开启：添加名称，默认扣分 -1f
        currentNames.add(deedName)
        currentPenalties.add(-1f)
    } else {
        // 关闭：按名称移除
        val idx = currentNames.indexOf(deedName)
        if (idx >= 0) {
            currentNames.removeAt(idx)
            currentPenalties.removeAt(idx)
        }
    }
    setDraft(_draft.value.copy(
        dailyMustDoDeedNames = currentNames,
        dailyMustDoDeedPenalties = currentPenalties,
    ))
}

fun updateDailyMustDoPenalty(deedName: String, penalty: Float) {
    val names = _draft.value.dailyMustDoDeedNames
    val penalties = _draft.value.dailyMustDoDeedPenalties.toMutableList()
    val idx = names.indexOf(deedName)
    if (idx >= 0) {
        penalties[idx] = penalty
        setDraft(_draft.value.copy(dailyMustDoDeedPenalties = penalties))
    }
}

fun isDailyMustDo(deedName: String): Boolean =
    deedName in _draft.value.dailyMustDoDeedNames

fun getDailyMustDoPenalty(deedName: String): Float {
    val idx = _draft.value.dailyMustDoDeedNames.indexOf(deedName)
    return if (idx >= 0) _draft.value.dailyMustDoDeedPenalties.getOrElse(idx) { -1f } else 0f
}
```

最后两个是 **convenience getter**，不是更新方法，不调用 `setDraft`。

## 4. SettingsScreen — 新增 DailyMustDoCard

在 `EventSettingsCard` 之后（当前的 line 195-202 之后）插入新 card：

```
=== 每日必做 ===
说明文字：「勾选需要在每日完成的善业，并设置未完成扣分」

[对于每一个 goodDeedPresets 中的善业]
  ┌────────────────────────────────────────┐
  │  [Switch]  帮助他人   未完成扣:[___]分  │
  │  [Switch]  早起早睡   未完成扣:[___]分  │
  │  ...                                    │
  └────────────────────────────────────────┘

底部提示：(只有善业可以被设为每日必做)
```

**每行布局（用户已确认）：**
- 左：Switch — 控制是否每日必做
- 中：善业名称文字
- 右：惩罚分值 OutlinedTextField（Switch 开启时可编辑，关闭时灰色/隐藏）

默认惩罚值设为 -1f（负值表示扣分）。

**组件签名：**
```kotlin
@Composable
private fun DailyMustDoCard(
    goodDeedPresets: List<String>,
    dailyMustDoDeedNames: List<String>,
    dailyMustDoDeedPenalties: List<Float>,
    onToggle: (String, Boolean) -> Unit,
    onPenaltyChange: (String, Float) -> Unit,
)
```

## 5. MainViewModel — vis 追踪

新增状态：

```kotlin
private val _dailyMustDoDoneToday = MutableStateFlow<Set<String>>(emptySet())
```

新增方法：

```kotlin
/** 标记某善业为今日已做 */
fun markDailyMustDoDone(eventName: String) {
    _dailyMustDoDoneToday.value = _dailyMustDoDoneToday.value + eventName
}

/** 查询 vis 值：0=未做，1=已做 */
fun getDailyMustDoVis(eventName: String): Int {
    return if (eventName in _dailyMustDoDoneToday.value) 1 else 0
}
```

在 `MainUiState` 新增：

```kotlin
val dailyMustDoDeedNames: List<String> = emptyList(),
val dailyMustDoDoneToday: Set<String> = emptySet(),
```

在 `MainViewModel.init` 的 combine 中透传：

```kotlin
dailyMustDoDeedNames = settings.dailyMustDoDeedNames,
dailyMustDoDoneToday = _dailyMustDoDoneToday.value,
```

**onConfirm 逻辑修改：**
在 `onConfirm()` 中成功添加记录后（当前 line 236-238），追加：

```kotlin
// 如果是每日必做善业，标记为今日已做
val confirmedEvent = rawEvent
if (confirmedEvent in _dailyMustDoDoneToday.value) {
    markDailyMustDoDone(confirmedEvent)
}
```

## 6. MainScreen — 传递数据

在 `MainScreen.kt` 的 EventPanel 调用处，新增参数：

```kotlin
dailyMustDoDeedNames = s.dailyMustDoDeedNames,
dailyMustDoDoneToday = s.dailyMustDoDoneToday,
```

EventPanel 组件签名增加这两个参数。

## 7. EventPanel — 善业颜色按 vis 变化

在 `EventPanel.kt` 的 `EventSection`（善业 section 调用处），以及 `EventSection` 内部：

**EventPanel 传入新参数：**
```kotlin
dailyMustDoDeedNames: List<String>,
dailyMustDoDoneToday: Set<String>,
```

**EventSection 新增参数：**
```kotlin
dailyMustDoDeedNames: List<String> = emptyList(),
dailyMustDoDoneToday: Set<String> = emptySet(),
```

**善业项目背景色逻辑修改：**
当前 line 310-316 是背景色判断。对于善业 section（title=="善业"）：

```kotlin
.background(
    when {
        isSelected -> Color(0xFFffd700).copy(alpha = 0.1f)
        // 每日必做判断
        event in dailyMustDoDeedNames && event !in dailyMustDoDoneToday -> Color(0xFF4488ff).copy(alpha = 0.15f)  // 未做蓝色
        event in dailyMustDoDeedNames && event in dailyMustDoDoneToday -> Color(0xFF69f0ae).copy(alpha = 0.08f)  // 已做绿色
        title == "善业" -> Color(0xFF69f0ae).copy(alpha = 0.08f)
        title == "恶业" -> Color(0xFFff5252).copy(alpha = 0.08f)
        title == "善果" -> Color(0xFFffd700).copy(alpha = 0.08f)
        else -> ScoreBtnBg
    }
)
```

善业文字颜色（line 332-341）也增加相应规则：
```kotlin
color = when {
    isSelected -> Color(0xFFffd700)
    !canSelect -> Color(0xFF666666)
    event in dailyMustDoDeedNames && event !in dailyMustDoDoneToday -> Color(0xFF88bbff)  // 蓝色文字
    else -> Color(0xFFa0c4ff)
}
```

---

## 关键设计决策

1. **数据存储**：两个平行列表 `(names, penalties)`，与善业 preesets 分离，避免索引偏移问题
2. **vis 变量**：`_dailyMustDoDoneToday: Set<String>`（内存态，每日自动重置），每个每日必做事件的 vis = `eventName in set ? 1 : 0`
3. **默认扣分**：`-1f`，用户可在输入框中修改（负值表示扣分）
4. **最大简洁度**：所有改动都在已有的文件模式内（data class + Room migration + ViewModel + Composable），不新增文件

## 验证方式

1. 在设置页面打开"每日必做"卡片，勾选几个善业并设置扣分值
2. 返回主页面，善业列表中对应项目显示蓝色背景
3. 选中该善业并点击「确认善行」
4. 该善业变为绿色背景（已做）
5. 重开应用检查设置是否保存（dailyMustDoDeedNames 应持久化）
