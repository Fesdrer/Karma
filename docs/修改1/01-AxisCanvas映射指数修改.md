# 修改日志 - 变更1

**文件：** `AxisCanvas.kt`
**日期：** 2026/06/15
**修改内容：** 将非线性映射指数从 0.6 改为 0.2314

## 修改详情

1. 新增常量 `AXIS_COMPRESSION_EXPONENT = 0.2314f`
2. 修改 `scoreToY` 函数中的两处 `pow(..., 0.6)` → `pow(..., AXIS_COMPRESSION_EXPONENT.toDouble())`

## 数学原理

要求：`+5` 分映射到 canvas 1/4 高度位置

解方程 `(5/100)^e = 0.5`：
- `e * ln(0.05) = ln(0.5)`
- `e = ln(0.5) / ln(0.05) ≈ 0.2314`

## 验证

- `AXIS_DISPLAY_RANGE = 100f` 保持不变
- 动画、背景、段位色带、指针逻辑不变
- 刻度算法不变（idealTicks=16）
