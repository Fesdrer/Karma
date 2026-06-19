# Karma v3.2 — UI 优化调整

## 改动 1: ScorePanel 分数始终显示

**问题**：刚打开时分数区域不显示，选择后才出现 + 轴下移抖动。

**方案**：移除 `if (selectedScore != null)` 条件，始终渲染分数文本，null 时显示 `0`（蓝色）。

**文件**：`ui/main/components/ScorePanel.kt`

---

## 改动 2: 历史图表去掉折线下方填充

**问题**：折线下方有金色半透明填充区域。

**方案**：删除 "Area fill under line" 整段代码块。

**文件**：`ui/history/HistoryChartCanvas.kt`

---

## 改动 3: 历史页面按钮合并为下拉菜单

**问题**：Row1 四个视图按钮 + Row2 三个导出按钮，过于拥挤。

**方案**：
- Row1 右侧：日/周/月/全部 → 下拉菜单（当前模式 + ▼）
- Row2 右侧：CSV/JSON/导入 → 下拉菜单（"导出 ▼"）
- 导航和放大功能不变

**文件**：`ui/history/HistoryScreen.kt`

### 新布局
```
Row 1: [← 返回] 历史记录       [日 ▼]
Row 2: [◀] 日期 [▶] [今天] [放大]    [导出 ▼]
Chart
```
