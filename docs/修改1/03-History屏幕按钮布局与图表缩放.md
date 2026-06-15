# 修改日志 - 变更3

**修改文件：**
- `HistoryScreen.kt` — 两行按钮布局
- `HistoryChartCanvas.kt` — 添加缩放+平移手势

**日期：** 2026/06/15

## 3a：6个按钮同时显示

原布局使用 `horizontalScroll` 滚动显示6个按钮。改为两行结构：

**第一行：** 左「← 返回 + 历史记录」| 右「日 周 月」
**第二行：** 右对齐「CSV JSON 导入」

按钮参数调整：
- 内边距 `horizontal = 12.dp` → `10.dp`, `vertical = 6.dp` → `5.dp`
- 字体 `14.sp` → `13.sp`
- 间距 `spacedBy(8.dp)` → `spacedBy(4.dp)`

删除不再使用的 import：`horizontalScroll`、`rememberScrollState`

## 3b：图表缩放 + 拖拽平移

### ChartViewport 扩展
- 新增 `minTimeRange: Double = 3600000.0`（最小缩放 1 小时）
- 新增 `maxTimeRange: Double = 0.0`（首次自动适配时设置为全范围）

### 手势实现
在 Canvas 上添加第二个 `.pointerInput(Unit)` 块，使用 `detectTransformGestures`：

- **双指捏合缩放**：`zoom != 1.0` 时触发，缩放中心为两个手指的 centroid
  - 将 centroid 屏幕坐标映射到时间坐标
  - 按 zoom 比例缩放可见时间范围
  - 约束到 `[minTimeRange, maxTimeRange]`
- **双指水平拖拽平移**：`pan.x > 2f` 时触发
  - 按比例将 pan 像素偏移转换为时间偏移
  - 约束不超出 `[0, maxTimeRange]`
- **自动适配 Y 轴**：缩放/平移后，根据可见区域的数据点重新计算 `yMin/yMax`
- **实时渲染**：用 `mutableStateOf(redrawTick)` 在 composable scope 中读取，修改后触发 Canvas 重绘

### 保持原有功能
- `detectTapGestures` 点击选择数据点 + 工具提示完全保留
- 自动适配首次 viewport 的逻辑不变
- 折线、点、面积填充绘制逻辑不变
