# 历史页导出/导入增加设置项

## 背景

历史页的导出/导入目前只处理事件记录（history entries），不包含任何设置项。用户希望导出/导入时同时包含设置页面的所有配置，包括三种事件类型（善事、恶事、善果）的预设编辑内容、颜色、字体、衰减参数、阶位名称/阈值、运气参数等全部设置。

## 方案

**只修改 JSON 格式的导出/导入，CSV 保持不变**（CSV 为表格格式，无法合理表达嵌套的设置结构）。

### 导出 JSON 新格式

在现有 JSON 中新增 `"settings"` 键，嵌入完整的 `KarmaSettingsEntity` 序列化对象。保留顶层 `"totalScore"` 和 `"history"` 以保证向后兼容。

```json
{
  "totalScore": 12.5,
  "settings": {
    "id": 1,
    "totalScore": 12.5,
    "scorePresets": [-2, -1.5, -1, -0.5, 0.5, 1, 1.5, 2],
    "goodDeedPresets": ["帮助他人", "早起早睡", "锻炼身体", "日行一善", "孝敬父母"],
    "badDeedPresets": ["发脾气", "浪费粮食", "口出恶言", "懒惰拖延", "伤害他人"],
    "goodResultPresets": [],
    "scoreAxisFontSize": 18,
    "scoreAxisRangeMin": -6,
    "scoreAxisRangeMax": 6,
    "axisLabelColor": -1,
    "axisTickThickness": 3,
    "axisLabelFontSize": 28,
    "axisDisplayRange": 100,
    "showNearbyTicks": false,
    "nearbyTickRange": 10,
    "axisQuarterValue": 30,
    "rankColors": [...],
    "historyLineThickness": 5,
    "historyDotRadius": 8,
    "guideLineWidth": 6,
    "guideLineColor": 4294967295,
    "decayEnabled": false,
    "decayHour": 23,
    "decayMinute": 0,
    "lastDecayDate": "",
    "rankDecayAmounts": [2, 2, 2, 2, 2, 3, 3, 3, 3],
    "rankThresholds": [10, 30, 60, 100, 150, 210, 280, 360],
    "rankNames": ["壹阶", "贰阶", "叁阶", "肆阶", "伍阶", "陆阶", "柒阶", "捌阶", "玖阶"],
    "luckEnabled": false,
    "luckT": 7,
    "luckB": 1,
    "luckW": 100
  },
  "history": [...]
}
```

### 导入逻辑

- JSON 导入时检测是否存在 `"settings"` 键
  - 存在（新格式）：用 Gson 将 `"settings"` 的 Map 转回 `KarmaSettingsEntity`，调用 `settingsDao.upsertSettings()` 整体替换
  - 不存在（旧格式）：保持原行为，只导入 `totalScore`，其他设置不变
- CSV 导入：完全不变，只处理历史记录

## 涉及文件

| 文件 | 修改类型 |
|---|---|
| `app/.../data/repository/KarmaRepository.kt` | 修改 `exportJson()` 和 `importJson()` |

**无需修改**：HistoryViewModel.kt、HistoryScreen.kt（接口不变）、CSV 相关代码

## 实施步骤

### Step 1：修改 exportJson()

添加 `"settings"` 键到导出数据中：

```kotlin
val gson = com.google.gson.Gson()
val root = com.google.gson.JsonObject()
root.addProperty("totalScore", settings.totalScore.toDouble())

// 将整个 KarmaSettingsEntity 序列化为嵌套对象
val settingsObj = gson.toJsonTree(settings).asJsonObject
root.add("settings", settingsObj)

// history 数组（现有逻辑）
val historyArray = com.google.gson.JsonArray()
for (entry in history) { ... }
root.add("history", historyArray)

return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)
```

### Step 2：修改 importJson()

在解析完 JSON 的 Map 后，检测 `"settings"` 键：

```kotlin
val settingsRaw = map["settings"]
if (settingsRaw != null) {
    // 新格式：通过中间 JSON 字符串转换，让 Gson 处理泛型类型推导
    val settingsJson = gson.toJson(settingsRaw)
    val importedSettings = gson.fromJson(settingsJson, KarmaSettingsEntity::class.java)
    settingsDao.upsertSettings(importedSettings)
} else {
    // 旧格式：只更新 totalScore
    val totalScore = (map["totalScore"] as? Number)?.toFloat() ?: return false
    val currentSettings = settingsDao.getSettingsOnce() ?: KarmaSettingsEntity()
    settingsDao.upsertSettings(currentSettings.copy(totalScore = totalScore))
}
```

关键注意点：
- Gson 反序列化时，`List<Long>`, `List<Float>`, `List<String>` 需要通过 `toJson(map["settings"])` → `fromJson(json, Entity::class.java)` 的间接路径，确保泛型类型被正确推导
- 当 `"settings"` 存在时，其内的 `totalScore` 也会被一并导入，不再需要独立处理顶层的 `totalScore`

### Step 3：确认 CSV 不修改

CSV 没有结构表达能力，不添加设置项。CSV 导入时仅处理历史记录，设置保持当前值不变。

## 向后兼容

- 旧格式 JSON（无 `"settings"`）：导入正常，只覆盖 `totalScore` 和历史记录
- 新格式 JSON（有 `"settings"`）：覆盖全部设置和历史记录
- 跨版本：未来的 KarmaSettingsEntity 新增字段时，用此方法导出的旧文件缺少该字段 → 使用 `fromJson` 反序列化时字段会取 Kotlin 默认值（即 data class 构造函数中声明的默认值），行为符合预期
- CSV 文件：完全不变

## 验证

1. 编译检查
2. 导出 JSON → 打开文件检查包含 `"settings"` 键，所有字段完整且类型正确
3. 手动构建一个不含 `"settings"` 的旧格式 JSON → 导入 → 只导入历史记录和总分，设置不变
4. 修改设置（改颜色、改事件预设、调参数）→ 导出 JSON → 清数据 → 导入新 JSON → 确认设置和历史记录都恢复
5. CSV 导出/导入功能正常（不受影响）
