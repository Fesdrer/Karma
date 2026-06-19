# Karma v3.2 Bug 修复 — 导航/视图/放大问题

## Bug 1: 周模式导出倒三角换行

**原因**：WEEK 模式日期标签（如 "第25周 (6/15-6/21)"）过长，撑宽左侧导航 Row，右侧 `导出 ▼` 被挤到下一行。

**文件**：`ui/history/HistoryScreen.kt` Row 2

**方案**：日期标签加 `Modifier.weight(1f, fill = false)` + `maxLines = 1` + `overflow = TextOverflow.Ellipsis`

---

## Bug 2: 改变日期后无法显示事件

**原因**：`remember(state.viewMode) { ChartViewport() }` 仅以 viewMode 为 key，导航改变 focusDate 后 viewport 不重建，auto-fit 不运行，数据点画在不可见区域。

**文件**：`ui/history/HistoryScreen.kt` 第 308 行

**方案**：remember key 改为 `remember(state.viewMode, state.focusDate) { ChartViewport() }`

---

## Bug 3: 放大按钮不能取消

**原因**：放大→点击钻取→进入 DAY 模式，此时 `zoomDisabled = true`（DAY 不可放大），按钮灰化但 `_isZoomEnabled` 仍为 true。

**文件**：`ui/history/HistoryViewModel.kt`

**方案**：进入 DAY 模式时自动关闭 zoom：
1. `zoomToPoint()` WEEK→DAY 分支设 `_isZoomEnabled.value = false`
2. `setViewMode()` 切到 DAY 时也设 `_isZoomEnabled.value = false`

---

## Bug 4: 全部页面没有放大按钮

**原因**：v3.2 合并时丢失了 ALL 模式的 else 分支。

**文件**：`ui/history/HistoryScreen.kt` Row 2

**方案**：在 `if (state.viewMode != ViewMode.ALL)` 后补 `else { 放大按钮 }`

---

## 修改文件汇总

| 文件 | Bug | 改动 |
|------|-----|------|
| `HistoryScreen.kt` | 1 | 日期标签加 weight + maxLines + overflow |
| `HistoryScreen.kt` | 2 | remember key 加 focusDate |
| `HistoryScreen.kt` | 4 | 补 ALL 模式 else 分支放大按钮 |
| `HistoryViewModel.kt` | 3 | zoomToPoint/setViewMode 进入 DAY 关 zoom |
