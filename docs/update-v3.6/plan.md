# 零依赖 HSV 滑块颜色选择器

## 背景

设置页面的颜色选择器（刻度颜色、指引线颜色、阶位颜色）目前是自实现的 R/G/B 数字输入框（`OutlinedTextField` × 3），交互不直观。之前尝试引入 `skydoves/colorpicker-compose` 库，但该库编译于 Kotlin 2.3.0，项目使用 Kotlin 1.9.0，版本不兼容。用户要求**不升级 SDK/Kotlin、不加新依赖**的解决方案。

## 方案

**使用 Material3 `Slider` 自建 HSV 取色器**（Slider 已存在于当前依赖中，无需加任何新库）：

- 色相滑块（Hue, 0–360°）
- 饱和度滑块（Saturation, 0–100%）
- 明度滑块（Value/Brightness, 0–100%）
- 实时色块预览
- 颜色转换使用 `android.graphics.Color.RGBToHSV()` / `HSVToColor()`（Android SDK 原生方法，零依赖）

## 涉及文件

| 文件 | 修改类型 |
|---|---|
| `app/.../ui/settings/SettingsScreen.kt` | 替换 `ColorPickerDialog` + 删除 `ColorTextField` + 新增 3 个辅助 Composable |

**无需修改**：`build.gradle.kts`、`libs.versions.toml`、`SettingsViewModel.kt`、三处调用方。

## 实施步骤

### Step 1：新增 import

SettingsScreen.kt 的 import 区域添加一行：

```kotlin
import androidx.compose.ui.graphics.Brush    // 滑块渐变色条
```

`Slider` 和 `SliderDefaults` 已存在 import（第37–38行），不重复添加。

### Step 2：替换 ColorPickerDialog（第959–1019行）

**函数签名不变**：`(currentColor: Long, onColorSelected: (Long) -> Unit, onDismiss: () -> Unit)`

内部实现替换为：

1. **初始 HSV 提取**（Long → HSV）：`android.graphics.Color.RGBToHSV(r, g, b, hsvArray)`
2. **三个可变状态**：`hue`(0f..360f), `saturation`(0f..1f), `value`(0f..1f)
3. **实时预览转换**（HSV → Long）：`android.graphics.Color.HSVToColor(...).toLong() and 0xFFFFFFFFL`
4. **AlertDialog 内容**：预览色块 + 三个滑块组件 + 确定/取消按钮
5. **主题一致**：`containerColor = Color(0xFF16213e)`、Gold 按钮

### Step 3：新增三个辅助 Composable

每个滑块组件包含：文本标签行（名称 + 当前值）+ 渐变色参考条（`Box` + `Brush.horizontalGradient`）+ `Slider`

- **`HueSliderComponent`** — 彩虹渐变色条，标签"色相"+角度
- **`SaturationSliderComponent`** — 灰→彩色渐变色条（依赖当前 hue/value），标签"饱和度"+百分比
- **`ValueSliderComponent`** — 黑→彩色渐变色条（依赖当前 hue/saturation），标签"明度"+百分比

所有 Slider 统一使用 `Gold` thumb/track 颜色，`BorderSubtle` 作为 inactive track 颜色。

### Step 4：删除 ColorTextField（第1021–1049行）

整体删除该 composable 函数。import 保留（其他组件可能在使用）。

## 关键转换

```
Long(0xAARRGGBB) → Int(r,g,b) → RGBToHSV → Float[]{h,s,v}
                                                      ↓
HSVToColor → Int(0xAARRGGBB) → .toLong() & 0xFFFFFFFFL → Long(0xAARRGGBB)
```

`HSVToColor` 返回的 `Int` 符号扩展通过 `and 0xFFFFFFFFL` 消除。

## 验证

编译后运行到测试机上，分别点击：
1. AxisSettingsCard → 刻度颜色色块 → 拖动色相 → 预览更新 → 确定
2. AxisSettingsCard → 指引线颜色色块 → 同上的流程
3. RankSettingsCard → 任意阶位颜色色块 → 同上的流程

边缘情况：灰色（sat=0）、黑色（val=0）时滑块正确初始化。

# 合并 HSV 滑块 + RGB 数字输入的颜色选择器

## 背景

当前的 `ColorPickerDialog` 只有 HSV 滑块一种输入方式。用户希望同时支持数字输入，让滑块和数字输入在同一个对话框中并存，R/G/B 数字输入框允许被删空（不再强约束为数字必须存在）。

## 方案

**在现有 HSV 三滑块下方，添加一行 R/G/B 数字输入框**，双向同步：

- 拖动滑块 → 数字框实时更新
- 输入数字 → 滑块同步移动
- 数字框允许空值（删除全部数字后预览停留在最后有效颜色）
- 无需选项卡/选择器，两种输入方式同时可见

## 涉及文件

| 文件 | 修改类型 |
|---|---|
| `app/.../ui/settings/SettingsScreen.kt` | 修改 `ColorPickerDialog`，新增 `RGBTextField` |

**无需修改**：`build.gradle.kts`、`libs.versions.toml`、`SettingsViewModel.kt`、三个调用方

## 实施步骤

### Step 1：修改 ColorPickerDialog

在现有 HSV 滑块下方，预览色块上方，添加一行 R/G/B 数字输入框。

状态设计：
- **来源**：HSV 是权威颜色状态（`hue`, `saturation`, `value`）
- **派生**：从 HSV 计算出 `currentR`, `currentG`, `currentB`（Int, 0–255）
- **文本**：每个通道有自己的 `text`（String），可从 current 值同步填充，也允许为空

同步逻辑：
- 滑块拖动 → 更新 HSV → 重新计算 `currentR/G/B` → 更新文本字段
- 文本输入 → 解析数字 → 若三个都有效则 `RGBToHSV` → 更新 `hue/saturation/value`
- 文本为空 → 不更新颜色，预览保持最后一次有效值

### Step 2：新增 RGBTextField

```kotlin
@Composable
private fun RGBTextField(
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
)
```

行为：
- 初始显示 `initialValue.toString()`
- 输入只允许数字（`.`filter `{ it.isDigit() }`）
- **允许空值**：用户可删除全部数字，文本为空时不调用 `onValueChange`
- 聚焦丢失（`onFocusChanged`）时若为空则恢复为 `initialValue.toString()`
- 宽度 `widthIn(min = 56.dp)`，紧凑布局

### Step 3：布局

```
+---------------------------+
| 选择颜色                  |
|                           |
|  [ 预览色块 64dp ]        |
|                           |
| 色相 ●--------  120°      |  ← 现有 HSV 滑块
| 饱和度 ●------  50%       |
| 明度 ●-------  80%        |
|                           |
| ──── 分隔线 ────          |
|                           |
| R [ 128 ] G [ 64 ] B [255] |  ← 新增 RGB 数字
|                           |
|         [确定] [取消]      |
+---------------------------+
```

## 关键转换

```
RGB数字 → toIntOrNull → if all valid: RGBToHSV → hue/sat/val  ←用户输入数字
HSV滑块 → HSVToColor → Int → extract r/g/b → 文本字段显示   ←用户拖滑块
                              ⇓
                    .toLong() & 0xFFFFFFFFL → Long(0xAARRGGBB) → preview
```

## 验证

1. 打开刻度颜色取色器 → 拖动色相滑块 → R/G/B 数字同步变化
2. 在 R 框中输入 "12" → 色相/饱和度/明度滑块和预览联动更新
3. 清空 R 框 → 预览颜色不变（停留在最后有效值）
4. 清空所有三个框 → 预览不变，点确定不崩溃
5. 边缘情况：从白色初始（255,255,255）→ 输入"0"→ 变黑；灰色饱和度为0时 hue 不变
