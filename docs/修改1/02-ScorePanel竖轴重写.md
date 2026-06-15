# 修改日志 - 变更2

**修改文件：**
- `ScorePanel.kt` — 完全重写
- `MainViewModel.kt` — 清理
- `MainScreen.kt` — 调整

**日期：** 2026/06/15

## ScorePanel.kt 重写

删除内容：
- `scorePresets: List<Float>` 参数（不再需要预设网格）
- `onEditClick: () -> Unit` 参数（编辑按钮已移除）
- `LazyVerticalGrid` 预设网格按钮
- 编辑按钮（"编辑" TextButton）

新增内容：
- `ScoreAxisView` — 内部 Canvas 竖向数轴组件
  - 范围 ±6，步长 0.5，均匀刻度
  - 主刻度线（整数位置，长 8px）+ 副刻度线（0.5 位置，短 5px）
  - 右侧文字标签：正数蓝色、负数红色、0 金色加粗
  - 红色实心圆（`#ff5252`）指示选中位置，带外发光和内高光
- 交互：`detectTapGestures` 点击选中 + `detectVerticalDragGestures` 拖拽
- 位置捕捉 `snapToHalf()`：`round(v * 2) / 2`，约束 ±6
- 保留自定义输入框

## MainViewModel.kt 清理

移除内容：
- `_scoreEditMode` / `scoreEditMode` 状态
- `_editModePair` combine（不再需要）
- `openScoreEdit()`, `closeScoreEdit()`, `saveScorePresets()` 方法
- `MainUiState.scoreEditMode` 字段

保留内容：
- `selectedScore` / `_selectedScore` — 仍由新竖轴选择
- `selectScore()` — 签名不变
- `onCustomScoreChanged()` — 保留
- `scorePresets` 保留在数据层，不触发数据库迁移

## MainScreen.kt 调整

- 移除 `ScoreEditModal` 导入和使用
- ScorePanel 调用参数：移除 `scorePresets`、`onEditClick`
