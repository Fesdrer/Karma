# 11 项外观修改

## 背景
统一整修应用的字体、背景、颜色、布局问题。

---

## 一、字体（衬线体）统一

### 涉及文件和修改模式

所有文件中，对未指定 `fontFamily` 的 Text 显式加 `fontFamily = FontFamily.Serif`。对 Canvas Paint 加 `typeface`。

#### 3. 大衍筮法全部字体

| 文件 | 修改 |
|------|------|
| `YarrowCanvas.kt` | `drawText` 调用处的 `TextStyle` 和 `Paint` 加 `fontFamily`/`typeface` |
| `YarrowResultPanel.kt` | 所有 `Text(...)` 加 `fontFamily = FontFamily.Serif` |
| `DivinationScreen.kt` | Yarrow 相关的 2 处 Text（"大衍之数五十"、"查看启示"）加 fontFamily |

#### 6. 小六壬全部字体

| 文件 | 修改 |
|------|------|
| `XiaoLiuRenInputPanel.kt` | `TraditionalLabel`、`OutlinedTextField` 的 `textStyle`、`Text` 按钮、Dropdown 项 — 全加 fontFamily |
| `XiaoLiuRenPillarCanvas.kt` | Canvas `Paint` 对象加 `typeface`（宫名 paint + 六神标签 paint） |
| `XiaoLiuRenResultPanel.kt` | 所有 `Text(...)` 加 `fontFamily = FontFamily.Serif` |

#### 7. 气运测试"开始"按钮
`DivinationScreen.kt` — "开始"/"再来一次" Text 加 `fontFamily = FontFamily.Serif`

#### 8. 设置页字体不一致

| 文件 | 位置 | 修改 |
|------|------|------|
| `SettingsScreen.kt` | ColorPickerDialog 色相/饱和度/明度标签 | 从 `bodySmall` 改为 `bodySmall.copy(fontFamily = FontFamily.Serif)` |
| `SettingsScreen.kt` | EventSection `OutlinedTextField` | 从 `bodySmall` 改为 `bodySmall.copy(fontFamily = FontFamily.Serif)` |

---

## 二、背景修改

### 1. 祈福页面背景
`PrayerScreen.kt` — 删掉外层 Box 的 `background(Color.Black.copy(alpha = 0.85f))`，让 Theme 渐变透出。表单卡片保留自身 `0xFF1A1A1A` 背景。

### 5. 大衍筮法启示页背景
`YarrowResultPanel.kt`:
- 外层遮罩：`Color.Black.copy(alpha = 0.7f)` → `Color.Transparent`（让渐变透出）
- 启示框内（Column 背景 `ChartBg`）：保持纯黑不变

---

## 三、大衍筮法筮棍颜色

`YarrowCanvas.kt` 第 72-77 行 `Color(0xFF4488ff)`（浅蓝）→ `Color(0xFF00FF00)`（翠绿）。两个辅助函数 `vs()` 和 `vh()` 中的颜色，以及第 89 行太极线同色。

---

## 四、状态栏遮盖

### 9. 页面顶部被状态栏遮住

| 文件 | 修改 |
|------|------|
| `HistoryScreen.kt` | 根 Column 加 `.statusBarsPadding()`（或 `.padding(top = ...)` 配合 `WindowInsets`） |
| `DivinationScreen.kt` | 检查是否需要加 statusBarsPadding |
| `PrayerScreen.kt` | 检查是否需要加 statusBarsPadding |

方法：在根布局上加 `Modifier.statusBarsPadding()`（需要 import `androidx.compose.foundation.layout.statusBarsPadding`）。

---

## 五、小六壬背景图片定位

### 10. 背景图片贴边
`XiaoLiuRenPillarCanvas.kt`:
- `dstOffset` 从 `IntOffset.Zero` 改为 `IntOffset(0, statusBarHeight)` — 顶部从状态栏下端开始
- `dstSize` 高度从 `screenH` 改为 `screenH - statusBarHeight`（底部贴合屏幕）
- 需要获取状态栏高度：通过 `WindowInsets.statusBars.asPaddingValues()` 或直接读取

---

## 六、数字键盘

### 11. 数字输入弹出数字键盘

| 文件 | 位置 | 修改 |
|------|------|------|
| `ScoreEditModal.kt` | 预设分值编辑 (line ~110) | 加 `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)` |
| `ScoreEditModal.kt` | 新增分数输入 (line ~151) | 同上 |
| `ScorePanel.kt` | 自定义分数输入 (line ~115) | 同上 |

---

## 不需要修改

- **第 2 项**：设置页渐入动画 — NavGraph.kt 中所有路由已有 `fadeIn(tween(300))`（v3.6-7 已修），设置和历史页面动画一致。如用户仍看到突变可能是缓存问题。

---

## 验证

1. `./gradlew assembleDebug` 编译通过
2. 打开到各页面检查字体是否为衬线体
3. 筮法棍子为翠绿色
4. 祈福/启示页背景渐变可见
5. 页面顶部不被状态栏遮盖
6. 小数输入弹出数字键盘
7. 小六壬背景图上下贴边
