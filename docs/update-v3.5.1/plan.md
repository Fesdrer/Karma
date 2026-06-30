
# 后续修改 — 善果/祈福段不参与积分

## 需求

当前积分中，每对相邻记录间的 K' 段都参与计算。但"善果"和"祈福"是消耗业力的行为，不是自然的业力波动——它们对应的斜率段应排除。

## 改动（仅一个文件 `util/LuckAmplifier.kt`）

### Point 增加 event 字段
`data class Point(val tDays: Double, val karma: Float, val event: String)`

### map 构建时保留 event
```kotlin
Point(tDays, entry.totalAfter, entry.event)
```

### 逐段积分时跳过
在每对相邻点 (pI, pJ) 中，pI 为较新点。如果 pI 的 event 以"善果："或"祈福："开头，跳过该段：
```kotlin
if (pI.event.startsWith("善果：") || pI.event.startsWith("祈福：")) continue
```

## 验证

编译通过；善果/祈福记录的斜率段不再计入运气波动积分。
